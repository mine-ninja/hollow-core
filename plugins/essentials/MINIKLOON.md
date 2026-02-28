![Preview](https://i.imgur.com/ILHcUwV.png)

```diff
-Naive solution:
```
1. Create a team, set its prefix or suffix to the component you want.
2. In the PlayerInfoUpdatePacket, set the Username to ""
3. Add "" to the team.

The player entity on the client stores the username from the player list entry with a matching UUID.

BUT when it looks up which team it's a part of, it also uses that username.
That causes a problem when you have multiple players, because they'll all be called "".
The fix is to use formatting codes, which render as whitespaces but can still be unique.

```diff
+Cool™ solution:
```
1. When the player is created, generate its "Tablist Username" and store it
2. In the PlayerInfoUpdatePacket, set the username to the Tablist Username, which may look like "§c§2§o§5§4".
3. Add "§c§2§o§5§4" to the team.

Code to generate the Tablist Username based on a counter (you can count Player instantiations with a static AtomicInteger):
```java
private static final char[] CODES = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'};
public static String getTablistUsername(int counter) {
    StringBuilder name = new StringBuilder();
    while(counter > 0) {
        int remainder = counter % CODES.length;
        name.append("§").append(CODES[remainder]);
        counter = counter / CODES.length;
    }
    return name.toString();
}
```