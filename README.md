# Reserved Slots Plugin
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=alert_status)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
[![Build and Test](https://github.com/PowerNukkit/Plugin-Reserved-Slots/actions/workflows/maven.yml/badge.svg)](https://github.com/PowerNukkit/Plugin-Reserved-Slots/actions/workflows/maven.yml)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=security_rating)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
<br/>
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=coverage)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=bugs)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=code_smells)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=PowerNukkit_Plugin-Reserved-Slots&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=PowerNukkit_Plugin-Reserved-Slots)


This plugin allows you to define how many player slots will be reserved for a group of players.

As example, you can use it to reserve slots:
- for VIP or donors
- for staff members
- for members with a specific permission

You can also allow members to join even if the server is full.

The messages are fully customizable.

## Cloning and importing
1. Just do a normal `git clone https://github.com/PowerNukkit/Plugin-Reserved-Slots.git` (or the URL of your own git repository)
2. Import the `pom.xml` file with your IDE, it should do the rest by itself

## Debugging
1. Create a zip file containing only the `plugin.yml` file
2. Rename the zip file to change the extension to jar
3. Create an empty folder anywhere, that will be your server folder.  
   <small>_Note: You don't need to place the PowerNukkit jar in the folder, your IDE will load it from the maven classpath._</small>
4. Create a folder named `plugins` inside your server folder  
   <small>_Note: It is needed to bootstrap your plugin, your IDE will load your plugin classes from the classpath automatically,
   so it needs to have only the `plugin.yml` file._</small>
5. Move the jar file that contains only the `plugin.yml` to the `plugins` folder
6. Create a new Application run configuration setting the working directory to the server folder and the main class to:  `cn.nukkit.Nukkit`  
![](https://i.imgur.com/NUrrZab.png)
7. Now you can run in debug mode. If you change the `plugin.yml` you will need to update the jar file that you've made.
