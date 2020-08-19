package minegame159.meteorclient.utils;

public class Version {
    private final int[] numbers;
    private final int beta;

    private final String string;

    private Version(int n1, int n2, int n3, int beta) {
        this.numbers = new int[] { n1, n2, n3 };
        this.beta = beta;

        if (beta == -1) this.string = String.format("%d.%d.%d", n1, n2, n3);
        else this.string = String.format("%d.%d.%d beta%d", n1, n2, n3, beta);
    }

    public boolean isHigherOrEqual(Version version) {
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] < version.numbers[i]) return false;
        }

        return true;
    }

    public boolean isLower(Version version) {
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] >= version.numbers[i]) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return string;
    }

    public static Version parse(String string) {
        String[] split = string.split(" ");
        String[] numbers = split[0].split("\\.");

        return new Version(
                parseInt(numbers[0]),
                parseInt(numbers[1]),
                parseInt(numbers[2]),
                split.length > 1 ? parseInt(split[1].substring(4)) : -1
        );
    }

    private static int parseInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
