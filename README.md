lwbd
====

A lightweight beat detection library written in Java.

lwbd takes mp3 files sampled at 44.1 khz (CD quality) and returns a collection of key-value pairs each containing the time in the song that a beat occurs and the value of the sample at that time.

lwbd is a pure Java implementation of Frédéric Patin's simple sound energy beat detection algorithm found here: http://www.flipcode.com/misc/BeatDetectionAlgorithms.pdf
As such it does not rely on the Java Media Framework (JMF) or javax. This means it is 100% portable; you can run it anywhere Java runs, including 
Android (which does not support JMF or javax).

lwbd uses JLayer for mp3 decoding and Minim for FFT.

It is not exceptionally fast. On a Nexus S beat detection for a 4 minute song takes about 4.5 minutes. On a x86 machine @ 1.7 GHz + 4gb DDR2 the same song takes 32 seconds on average. Speed is sacrificed for portability. If speed is a necessity, replace JLayer with a decoder written in native code.
