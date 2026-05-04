![Lines](https://img.shields.io/badge/dynamic/json?url=https://gist.githubusercontent.com/Lampalon142/c398eb39d86a2f8a0cd9152209391547/raw/lines.json&query=$.lines&color=black&label=lines&logo=github)

# LifeMod

## Introduction

LifeMod is a powerful moderation plugin for Minecraft servers, providing essential tools for server management and player oversight. Compatible with Minecraft versions 1.8 through 1.20, LifeMod enables server administrators and moderators to efficiently maintain order and fairness using a comprehensive set of commands.

## Project Overview

LifeMod focuses on delivering practical, command-based moderation tools without unnecessary complexity. The plugin is easy to install, supports a wide range of Minecraft versions, and is maintained by a single developer. The source code is available on GitHub for transparency, but there is currently no active community around the project.

## Key Features
Multi-version support: Works with Minecraft 1.8 to 1.20.
Command-based moderation: All tools are accessed via commandsnothingno GUI or interactive menus.
Essential moderation actions: Includes commands for freezing, vanishing, teleporting, inventory management, and more.
Open-source: Public codebase for transparency and potential improvements.

## Main Commands and Permissions :

### Command - Aliases - Description - Permission

/mod - staff - Moderate your server. - lifemod.mod  
/broadcast - bc - Produces a message for the entire server. - lifemod.bc  
/gm - gamemode - Changes the game mode. - lifemod.gm  
/fly - nothing - Allows you to fly. - lifemod.fly  
/ecopen - nothing - Open a person’s Ender Chest. - lifemod.ecopen  
/vanish - v - Vanish yourself or another player. - lifemod.vanish  
/clearinv - nothing - Clear inventory for a player or yourself. - lifemod.clearinv  
/stafflist - nothing - View moderators online. - lifemod.stafflist  
/staffchat - nothing - Chatting with other staff. - lifemod.staffchat  
/chatclear - nothing - To clear the chat. - lifemod.chatclear  
/togglechat - nothing - To activate or deactivate the chat. - lifemod.togglechat  
/heal - nothing - To heal a player or yourself. - lifemod.heal  
/tp - nothing - To teleport to a person. - lifemod.tp  
/tphere - nothing - Teleport a person to you. - lifemod.tphere  
/weather - nothing - To change the weather. - lifemod.weather  
/god - nothing - To activate or deactivate god mode for invincibility. - lifemod.god  
/freeze - nothing - To freeze and unfreeze a player. - lifemod.freeze  
/invsee - nothing - To see the inventory of a player. - lifemod.invsee  
/feed - nothing - To feed yourself or another player. - lifemod.feed  
/lifemod - nothing - Use Lifemod for help or reload. - lifemod.lifemod  
/speed - nothing - Update your speed. - lifemod.speed  
/spectate - nothing - See a player from a command. - lifemod.spectate  
/otp - nothing - Teleport to an offline player. - lifemod.otp  
/oinvsee - nothing - See the inventory of an offline player. - lifemod.oinvsee  
/hearts - nothing - Manage player hearts: set or add a specific amount of hearts to any player. - lifemod.hearts  
/settime - nothing - Quickly set the world time to day, night, noon, or midnight. - lifemod.time  
/difficulty - nothing - Change the server difficulty to peaceful, easy, normal, or hard. - lifemod.difficulty  
/report - nothing - Report a player. - nothing
/reports - nothing - View all reports. - lifemod.reports  


## Permissions Management
 Permissions can be assigned using your permissions plugin (such as LuckPerms, PermissionsEx, etc.).

## Technical Details
Partial GUI: Only /reports have a GUI for the reports -- other moderation actions require typed
Lightweight: No dependencies on external APIs, ensuring stability across supported versions.
