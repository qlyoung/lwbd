lwbd
====

*A lightweight beat detection library written in Java, designed for*
*simplicity and portability.*

What it does
------------
lwbd takes mp3 files sampled at 44.1 khz and returns a collection of 
Beat objects. Each Beat object contains the time in the song that a beat
occurs and the instantaneous sound energy at that time.

lwbd is a pure Java implementation of Frédéric Patin's Beat Detection 
Algorithm #1 (Simple Sound Energy) [found here](
http://www.flipcode.com/misc/BeatDetectionAlgorithms.pdf).
As such it does not rely on the Java Media Framework (JMF) or javax.
This means it is 100% portable; you can run it anywhere Java runs,
including Android (which does not support JMF or javax).

Note that mp3 decoding is a very complex and cpu-intensive task,
and Java is not particularly fast. To guarantee portability lwbd uses a
decoder written in Java. Hence, lwbd is not exceptionally fast.

On a Nexus S beat detection for a 4 minute song takes about 4.5 minutes.
On a typical Vista-era laptops the same song takes about 7 seconds.
*Speed is sacrificed for portability.* If speed is a necessity, replace
JLayer with a decoder written in native code. I have done this for one
of my projects and achieved analysis times of ~2 seconds for the same
song.

*lwbd can:*
* detect beats in a song with arbitrary sensitivity
* run on Android

*lwbd cannot:*
* find the beats per minute (BPM) of a song
* run quickly on Android (unless you replace JLayer with a decoder
  written in C *cough* libmpg123 *cough*)

lwbd uses JLayer for mp3 decoding and Minim for FFT.

*licensed under GPLv3*
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
[GNU General Public License](http://www.gnu.org/licenses/)
for more details.

