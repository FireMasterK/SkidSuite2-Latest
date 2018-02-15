# SkidSuite2
A set of projects focusing on java reverse engineering using ASM.

# Projects:

## SkidCore
The base library for all the projects. 

## SkidAntiOb
Targeted at deobfuscating obfuscated programs. This includes intelligent class renaming & automated patching of string encryption of known obfuscators such as ZKM / Allatori / DashO.

## SkidScan
Searches for class/method usage that can be used in a potentially malicous manner.

## SkidAnalysis
Targeted at stack analysis.

## SkidVisualizer
A tool used for finding object usage in a program. It can decompile classes using ASM & procyon.

## SkidHijack
A java agent allowing the rewriting and access of any class at runtime. 
*(Requires 'tools.jar' in classpath)*

## SkidObfuscator
A obfuscator offering renaming(classes / fields / method / local variables ) and string encryption. 

# Images

Imgur Album*(May be outdated)*: http://imgur.com/a/PrVtq

# Libraries used:
*(Included in SkidCore src)*
* [ASM 5](http://asm.ow2.org/)
* [Commons-IO](https://commons.apache.org/proper/commons-io/)

*(Not included)*
* [Procyon](https://bitbucket.org/mstrobel/procyon/)