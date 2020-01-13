# QuickerCrafting
A Minecraft 1.12 mod that implements factorio style crafting. 

All the recipes you can make from your inventory are displayed directly, rather than fussing around with a craft matrix. Gets rid of a lot of tedium I experience when playing modpacks. Built so you can focus on the factory, rather than micromanaging your inventory.

Note: This only changes player crafting, it does not change any autocrafting machines (e.g. buildcraft, AE2, etc..)

[Curseforge](https://www.curseforge.com/minecraft/mc-mods/quickercrafting)

## Features
- Displays all (non-advanced) recipes the player can craft with their inventory in a 3x3 matrix (either at all times or based on distance to a nearby crafting table)
- A GUI built on a "no-surprises" principle.
- Multiple craft result slots, so you can chain craft easily (i.e. logs -> planks -> sticks)
- Search all craftable recipes using a similar implementation as JEI
- Speedy craftable recipes generation on big modpacks with lots of items and recipes (~5 ms to populate recipes)
- Handy tooltips that show you exactly what and how many materials will be used in crafting and what slots it will take from.
- Compatibility with InvTweaks and Baubles

## Todo
- GUI Improvements (resizability, etc.) 
- Multi-stage crafting (i.e. you can craft stairs directly from logs by logs->planks->stairs)
- More mod integration and support (Chisel, more JEI support, mods that change the inventory, etc.)

## Pictures/Videos
![Interface](https://i.imgur.com/nx09yQf.png)

[The Menu and Quick Crafting](https://i.imgur.com/Ok8Gsb9.mp4)

[Search features and Scrolling](https://i.imgur.com/Ro8PZsb.mp4) <- Note this is before I implemented JEI's suffixtree implementation

