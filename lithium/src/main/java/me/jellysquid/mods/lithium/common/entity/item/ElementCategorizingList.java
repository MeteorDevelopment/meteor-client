package me.jellysquid.mods.lithium.common.entity.item;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.util.function.LazyIterationConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public abstract class ElementCategorizingList<T, Category> extends AbstractList<T> {
    private static final int CATEGORY_DOWNGRADE_THRESHOLD = 15;
    private static final int CATEGORY_UPGRADE_THRESHOLD = 20;
    private final ArrayList<T> delegate;
    private final ArrayList<T> delegateWithNulls;
    private final Reference2ReferenceOpenHashMap<Category, IntArrayList> elementsByType;
    private final Reference2ReferenceOpenHashMap<Category, IntArrayList> elementsByTypeA;
    private final Reference2ReferenceOpenHashMap<Category, IntArrayList> elementsByTypeB;

    private int modCount; //Only used for better error messages / ConcurrentModificationException

    public ElementCategorizingList(ArrayList<T> delegate) {
        this.delegate = delegate;
        this.delegateWithNulls = new ArrayList<>(this.delegate.size());
        this.elementsByType = new Reference2ReferenceOpenHashMap<>();
        this.elementsByTypeA = new Reference2ReferenceOpenHashMap<>();
        this.elementsByTypeB = new Reference2ReferenceOpenHashMap<>();
    }
    protected void initialize() {
        this.initializeInternal();
    }

    abstract Category getCategory(T element);
    abstract boolean areSubcategoriesAlwaysEmpty(Category category);
    abstract boolean isSubCategoryA(T element);
    abstract boolean isSubCategoryB(T element);
    abstract void onElementSubcategorized(T element, int index);
    abstract void onElementUnSubcategorized(T element);
    abstract void onCollectionReset();
    abstract T castOrNull(Object element);

    @SuppressWarnings("unused")
    //TODO implement hoppers and hopper minecarts using item entity categorization.
    // This may require a lot of work for compatibility with the item entity movement tracking and block entity sleeping
    @SafeVarargs
    public final void consumeCategories(LazyIterationConsumer<T> elementConsumer, Category... categories) {
        IntArrayList[] categoryLists = new IntArrayList[categories.length];
        int totalSize = 0;
        int numCategories = 0;
        for (Category category : categories) {
            IntArrayList categoryList = this.elementsByType.get(category);
            if (!categoryList.isEmpty()) {
                categoryLists[numCategories++] = categoryList;
                totalSize += categoryList.size();
            }
        }

        if (numCategories <= 1) {
            this.consumeElements(elementConsumer, categoryLists[0]);
            return;
        }

        int[] categoryListIndices = new int[categoryLists.length];

        //Get the elements from each category, merged using the overall order
        for (int i = 0; i < totalSize; i++) {
            int smallestIndex = -1;
            int smallestIndexCategory = -1;
            for (int j = 0; j < numCategories; j++) {
                int categoryListIndex = categoryListIndices[j];
                if (categoryListIndex < categoryLists[j].size()) {
                    if (smallestIndex == -1 || categoryLists[j].getInt(categoryListIndex) < smallestIndex) {
                        smallestIndex = categoryLists[j].getInt(categoryListIndex);
                        smallestIndexCategory = j;
                    }
                }
            }
            if (smallestIndexCategory == -1) {
                throw new IllegalStateException("No more elements to get!");
            }
            categoryListIndices[smallestIndexCategory]++;
            T element = this.delegateWithNulls.get(smallestIndex);
            LazyIterationConsumer.NextIteration next = elementConsumer.accept(element);
            if (next.shouldAbort()) {
                return;
            }
        }
    }

    @SuppressWarnings("unused")
    public LazyIterationConsumer.NextIteration consumeCategory(LazyIterationConsumer<T> elementConsumer, Category category) {
        IntArrayList categoryList = this.elementsByType.get(category);
        return this.consumeElements(elementConsumer, categoryList);
    }

    public LazyIterationConsumer.NextIteration consumeCategoryA(LazyIterationConsumer<T> elementConsumer, Category category) {
        IntArrayList categoryList = this.elementsByTypeA.get(category);
        if (categoryList == null && !this.areSubcategoriesAlwaysEmpty(category)) {
            categoryList = this.elementsByType.get(category);
        }

        return this.consumeElements(elementConsumer, categoryList);
    }

    public LazyIterationConsumer.NextIteration consumeCategoryB(LazyIterationConsumer<T> elementConsumer, Category category) {
        IntArrayList categoryList = this.elementsByTypeB.get(category);
        if (categoryList == null && !this.areSubcategoriesAlwaysEmpty(category)) {
            categoryList = this.elementsByType.get(category);
        }

        return this.consumeElements(elementConsumer, categoryList);
    }

    private LazyIterationConsumer.NextIteration consumeElements(LazyIterationConsumer<T> elementConsumer, IntArrayList categoryList) {
        if (categoryList == null) {
            return LazyIterationConsumer.NextIteration.CONTINUE;
        }
        int expectedModCount = this.modCount;
        int size = categoryList.size();
        for (int i = 0; i < size; i++) {
            if (expectedModCount != this.modCount) {
                throw new ConcurrentModificationException("Collection was modified during iteration!");
            }

            int index = categoryList.getInt(i);
            T element = this.delegateWithNulls.get(index);

            //The consumer must not modify the consumed element and or other elements in the collection, or must return ABORT.
            LazyIterationConsumer.NextIteration next = elementConsumer.accept(element);
            if (next != LazyIterationConsumer.NextIteration.CONTINUE) {
                return next;
            }
        }
        return LazyIterationConsumer.NextIteration.CONTINUE;
    }

    private void initSubCategories(Category category, IntArrayList source) {
        if (this.areSubcategoriesAlwaysEmpty(category)) {
            return;
        }

        IntArrayList categoryListA = new IntArrayList();
        IntArrayList categoryListB = new IntArrayList();
        this.elementsByTypeA.put(category, categoryListA);
        this.elementsByTypeB.put(category, categoryListB);

        for (int i = 0; i < source.size(); i++) {
            int elementIndex = source.getInt(i);
            T element = this.delegateWithNulls.get(elementIndex);
            if (isSubCategoryA(element)) {
                categoryListA.add(elementIndex);
            }
            if (isSubCategoryB(element)) {
                categoryListB.add(elementIndex);
            }
            this.onElementSubcategorized(element, elementIndex);
        }
    }

    private void removeSubCategories(Category category) {
        IntArrayList remove = this.elementsByTypeA.remove(category);
        if (remove == null) {
            return; //Sub categories weren't initialized, skip the rest
        }
        this.elementsByTypeB.remove(category);
        IntArrayList categoryList = this.elementsByType.get(category);
        for (int index : categoryList) {
            this.onElementUnSubcategorized(this.delegateWithNulls.get(index));
        }
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.delegate.contains(o);
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return this.delegate.toArray();
    }

    @NotNull
    @Override
    public <U> U @NotNull [] toArray(U @NotNull [] a) {
        return this.delegate.toArray(a);
    }

    @Override
    public boolean add(T element) {
        addInternal(element);
        return this.delegate.add(element);
    }

    private void addInternal(T element) {
        this.modCount++;

        Category category = this.getCategory(element);
        int index = this.delegateWithNulls.size();
        this.delegateWithNulls.add(element);
        IntArrayList categoryList = this.elementsByType.computeIfAbsent(category, e -> new IntArrayList());
        categoryList.add(index);

        if (categoryList.size() > CATEGORY_DOWNGRADE_THRESHOLD) {
            IntArrayList categoryListA = this.elementsByTypeA.get(category);
            if (categoryListA != null) {
                if (isSubCategoryA(element)) {
                    categoryListA.add(index);
                }
                if (isSubCategoryB(element)) {
                    IntArrayList categoryListB = this.elementsByTypeB.get(category);
                    categoryListB.add(index);
                }
                this.onElementSubcategorized(element, index);
            } else if (categoryList.size() > CATEGORY_UPGRADE_THRESHOLD) {
                this.initSubCategories(category, categoryList);
            }
        }

        if (this.delegateWithNulls.size() == Integer.MAX_VALUE) { //This can only when delegate is at least half full
            throw new IllegalStateException("Internal list size hit Integer.MAX_VALUE");
        }
    }

    @Override
    public boolean remove(Object o) {
        boolean remove = this.delegate.remove(o);
        T element;
        if (remove && (element = this.castOrNull(o)) != null) {
            this.removeInternal(element);
        }
        return remove;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return this.delegate.containsAll(c);
    }

    @Override
    public void clear() {
        this.delegate.clear();
        this.resetInternal();
        this.initializeInternal();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return this.delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public T get(int index) {
        return this.delegate.get(index);
    }

    @Override
    public T set(int index, T element) {
        T previous = this.delegate.set(index, element);
        if (previous != element) {
            this.modCount++;

            Category previousCategory = this.getCategory(previous);
            Category category = this.getCategory(element);
            //Fix the indices stored in elementsByType, elementsByTypeA and elementsByTypeB.
            int internalIndex = this.delegateWithNulls.indexOf(previous);
            this.delegateWithNulls.set(internalIndex, element);

            boolean subCategoryA = isSubCategoryA(element);
            boolean subCategoryB = isSubCategoryB(element);

            boolean prevSubCategoryA = isSubCategoryA(previous);
            boolean prevSubCategoryB = isSubCategoryB(previous);

            if (this.isSubCategorized(previousCategory)) {
                this.onElementUnSubcategorized(previous);
            }
            this.updateCategoryAndSubcategories(category, previousCategory, internalIndex, subCategoryA, prevSubCategoryA, subCategoryB, prevSubCategoryB);
            if (this.isSubCategorized(category)) {
                this.onElementSubcategorized(element, internalIndex);
            }
        }
        return previous;
    }

    private boolean isSubCategorized(Category previousCategory) {
        return this.elementsByTypeA.containsKey(previousCategory);
    }

    void updateCategoryAndSubcategories(Category category, Category previousCategory, int internalIndex, boolean subCategoryA, boolean prevSubCategoryA, boolean subCategoryB, boolean prevSubCategoryB) {
        boolean changedCategory = category != previousCategory;
        boolean changedA = changedCategory || (subCategoryA != prevSubCategoryA);
        boolean changedB = changedCategory || (subCategoryB != prevSubCategoryB);

        if (!changedCategory && !changedA && !changedB) {
            return; //We don't have to update any indices because nothing changed
        }

        this.modCount++;

        IntArrayList previousCategoryList = this.elementsByType.get(previousCategory);
        IntArrayList categoryList = this.elementsByType.computeIfAbsent(category, e -> new IntArrayList());

        if (changedCategory) {
            previousCategoryList.rem(internalIndex);
            int binarySearchIndex = Collections.binarySearch(categoryList, internalIndex);
            binarySearchIndex = -(binarySearchIndex + 1); //Get insertion location according to Collections.binarySearch
            categoryList.add(binarySearchIndex, internalIndex);
        }

        //Remove from elementsByTypeA and elementsByTypeB.
        if (previousCategoryList.size() >= CATEGORY_DOWNGRADE_THRESHOLD) {
            this.removeFromSubCategories(previousCategoryList, previousCategory, internalIndex, prevSubCategoryA && changedA, prevSubCategoryB && changedB, changedCategory);
        }

        //Add to elementsByTypeA and elementsByTypeB
        this.addToSubCategories(categoryList, category, internalIndex, subCategoryA && changedA, subCategoryB && changedB, changedCategory);
    }

    void removeFromSubCategories(Category previousCategory, int internalIndex, boolean removeFromA, boolean removeFromB, @SuppressWarnings("SameParameterValue") boolean allowRemoval) {
        this.modCount++;
        this.removeFromSubCategories(this.elementsByType.get(previousCategory), previousCategory, internalIndex, removeFromA, removeFromB, allowRemoval);
    }

    private void removeFromSubCategories(IntArrayList previousCategoryList, Category previousCategory, int internalIndex, boolean removeFromA, boolean removeFromB, boolean allowRemoval) {
        if (allowRemoval && previousCategoryList.size() == CATEGORY_DOWNGRADE_THRESHOLD) {
            this.removeSubCategories(previousCategory);
        } else {
            IntArrayList categoryListA = this.elementsByTypeA.get(previousCategory);
            if (categoryListA != null) {
                if (removeFromA) {
                    categoryListA.rem(internalIndex);
                }
                if (removeFromB) {
                    this.elementsByTypeB.get(previousCategory).rem(internalIndex);
                }
            }
        }
    }

    void addToSubCategories(Category category, int internalIndex, boolean subCategoryA, boolean subCategoryB, @SuppressWarnings("SameParameterValue") boolean allowCreation) {
        this.modCount++;
        IntArrayList categoryList = this.elementsByType.computeIfAbsent(category, e -> new IntArrayList());
        this.addToSubCategories(categoryList, category, internalIndex, subCategoryA, subCategoryB, allowCreation);
    }

    private void addToSubCategories(IntArrayList categoryList, Category category, int internalIndex, boolean addToCategoryA, boolean addToCategoryB, boolean allowCreation) {
        if (categoryList.size() > CATEGORY_DOWNGRADE_THRESHOLD) {
            IntArrayList categoryListA = this.elementsByTypeA.get(category);
            if (categoryListA != null) {
                if (addToCategoryA) {
                    int binarySearchIndex = Collections.binarySearch(categoryListA, internalIndex);
                    binarySearchIndex = -(binarySearchIndex + 1); //Get insertion location according to Collections.binarySearch
                    categoryListA.add(binarySearchIndex, internalIndex);
                }
                if (addToCategoryB) {
                    IntArrayList categoryListB = this.elementsByTypeB.get(category);
                    int binarySearchIndex = Collections.binarySearch(categoryListB, internalIndex);
                    binarySearchIndex = -(binarySearchIndex + 1); //Get insertion location according to Collections.binarySearch
                    categoryListB.add(binarySearchIndex, internalIndex);
                }
            } else if (allowCreation && categoryList.size() > CATEGORY_UPGRADE_THRESHOLD) {
                this.initSubCategories(category, categoryList);
            }
        }
    }

    @Override
    public T remove(int index) {
        T removed = this.delegate.remove(index);
        this.removeInternal(removed);
        return removed;
    }

    private void removeInternal(T element) {
        this.modCount++;

        Category category = this.getCategory(element);
        IntArrayList categoryList = this.elementsByType.get(category);
        int index = this.delegateWithNulls.indexOf(element);
        if (index != this.delegateWithNulls.size() - 1) {
            this.delegateWithNulls.set(index, null); //Set to null so the indices in the category lists stay valid
        } else {
            this.delegateWithNulls.remove(index);
        }

        categoryList.rem(index);
        if (categoryList.size() >= CATEGORY_DOWNGRADE_THRESHOLD) {
            if (categoryList.size() == CATEGORY_DOWNGRADE_THRESHOLD) {
                this.removeSubCategories(category);
            } else {
                IntArrayList categoryListA = this.elementsByTypeA.get(category);
                if (categoryListA != null) {
                    categoryListA.rem(index);
                    this.elementsByTypeB.get(category).rem(index);
                    this.onElementUnSubcategorized(element);
                }
            }
        }

        this.checkResize();
    }

    private void checkResize() {
        int size = this.delegateWithNulls.size();
        if (size > 64 && size > this.delegate.size() * 2) {
            this.resetInternal();
            this.initializeInternal();
        }
    }

    private void resetInternal() {
        this.onCollectionReset();
        this.delegateWithNulls.clear();
        this.elementsByType.clear();
        this.elementsByTypeA.clear();
        this.elementsByTypeB.clear();
    }

    private void initializeInternal() {
        for (T element : this.delegate) {
            this.addInternal(element);
        }
    }

    @Override
    public int indexOf(Object o) {
        return this.delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.delegate.lastIndexOf(o);
    }

    @Override
    public <U> U[] toArray(IntFunction<U[]> generator) {
        return this.delegate.toArray(generator);
    }

    @Override
    public Stream<T> stream() {
        return this.delegate.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return this.delegate.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        this.delegate.forEach(action);
    }
}
