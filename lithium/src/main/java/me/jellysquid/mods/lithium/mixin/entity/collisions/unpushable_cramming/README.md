# Entity Cramming Optimization

In this text ladders represent all ladder-like climbable blocks.

Entities that are inside ladders usually do not push each other. However, required calculations that are done still
cause lag. This optimization aims to remove this lag, while not changing any observable behavior. The performance of
should not be reduced significantly in other scenarios.

## Clearly observable behavior

Entities push each other when they collide. Some entities are not pushable, some are not pushable under certain
conditions. E.g. horses cannot be pushed when they have a rider. Item frames cannot be pushed.

Most monsters are not pushable when they are inside a ladder. However, monsters that are inside a ladder can still push
other entities.

When entities push other entities, they also push themselves in the opposite direction. Monsters that can climb ladders
will climb ladders when being pushed against a wall when inside their feet center position is in a ladder.

## Obscure observable behavior

The check whether monsters are in a ladder uses the entities' feet position block cache. The cache is cleared every time
the entity is ticked or changes block position. The cache is populated when it is accessed. The cache is also used for
other things, e.g. some powder snow interaction. The cache is *NOT* cleared when the block at the position is changed.
Usage of the outdated cache is possible and does happen. (How to reproduce: Place two monsters in a ladder, wait 2+
ticks, break the ladder. The first mob will fail to push the 2nd mob once, because the 2nd mob's cache hasn't been
invalidated when the first mob ticks.)

## The performance issue

When colliding with other entities, the list of collided entities is collected. After checking the bounding box
intersection test, the pushability test is done (scoreboard teams, cannot be pushed due to climbing, etc.). In the case
of lots of entities in the same ladder the pushability test will always fail. The bounding box intersection and the
pushability test are evaluated by each entity for each entity nearby. The amount of work done grows quadratically with
the number of entities nearby. Placing more than 100 entities in a ladder leads to an unreasonable amount of lag.

## The optimization

Keep track of which entities cannot be pushed due to being in a ladder. Skip these entities before the bounding box and
pushability test. The quadratic amount of work should no longer be needed.

### Challenges

1. Not affect the obscure vanilla behavior.
1. Somehow keep the cached information correct at all times.
1. Deal with entity types that do not use the climbing criteria for their pushability test.
1. Avoid a performance hit in other cases (entities spread out, no ladder, etc.)

### Solutions

1. Do not change the access pattern of the feet block position cache at all costs.
1. Update the cached information when the feet block position cache changes.
1. Use Reflection to analyse the entity classes when they first appear to find out whether any relevant method is
   overridden. Hardcode the behavior for vanilla entity classes, but be conservative about modded (sub-)classes that
   override the methods.
1. Only keep track of extra information when there are lots of entities touching each other inside a ladder in the given
   16x16x16 entity section.