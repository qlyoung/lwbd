lwbd
====

### This project is no longer maintained.
You may be interested in:
- https://github.com/deezer/spleeter
- https://github.com/chrvadala/music-beat-detector

Unfortunately, neither of these are easy to use on Android. If anyone wants to
take over this project please let me know.

------

*A lightweight beat detection library designed for simplicity and portability*

What it does
------------
lwbd detects rhythmic onsets (beats) in digital audio. Feed it an audio file,
and lwbd will provide you with the time location and sound energy of each beat
it detects.

Platforms & Formats
-------------------
lwbd ultimately aims to support all major audio formats. However, due to runtime
compatibility issues, some formats are currently unsupported on certain platforms.

Regardless of encoding, 44.1kHz is the only sample rate supported. lwbd assumes
all audio it receives is sampled at 44.1 kHz, so if you pass it data of the wrong
sample rate, you'll get garbage output.

**Format availability by platform:**

| Format     | Android         | Desktop (JavaSE) |
| :-----     | :-------------: | :--------------: |
| MP3        | Yes             | Yes              |
| Ogg Vorbis | In Progress     | In Progress      |
| AAC        | No              | No               |
| FLAC       | No              | In Progress      |
| WMA        | No              | No               |
| AIFF       | No              | No               |

Usage
-----
lwbd's capabilities are accessed through a single method, BeatDetector.detectBeats().
```java
File audioFile = new File("/path/to/audiofile.mp3");
Beat[] beats = BeatDetector.detectBeats(audioFile, AudioType.MP3);
```
BeatDetector provides overloads of this method that support additional input sources
and options. See src/v4lk/lwbd/Examples.java for examples.

Contributing
------------
Any audio library written in Java must take special care with respect to
portability, because some platforms (specifially Android) do not implement
the Java Sound API. Most audio libraries in Java are inextricably dependent
on this API, and as a result, only work in environments that provide the full
JavaSE runtime. I have taken great pains to ensure the compatibility of lwbd's
core with all major subsets of Java, but it simply isn't feasible for me to write
a platform-independent decoder from scratch for each format I want to support.

Instead, lwbd's strategy is to pilfer the sources for external decoder libraries,
wrap them for compatibility, and section them off by platform. None of this is
exposed to client code; for (lots of) convenience, lwbd handles decoding audio
behind the scenes. Decoders are located in ```v4lk.lwbd.decoders```. The pilfered
sources they wrap are in ```v4lk.lwbd.decoders.processing```. If you want to add
support for new formats or platforms, the process is:

1. Fork lwbd.
2. Implement the interface ```v4lk.lwbd.decoders.Decoder``` to add your new
   decoder.
3. Send me a pull request. If it works, I'll merge it in.

The interface only has one method and is extensively Javadoc'd. I've tried to

Technical
---------
lwbd's beat detection algorithm is an implementation of Frédéric Patin's Frequency
Selected Sound Energy Algorithm #1, found in his excellent paper
["Beat Detection Algorithms."](http://www.flipcode.com/misc/BeatDetectionAlgorithms.pdf)

License
-------
```
Copyright (C) 2015  Quentin Young

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.
```
