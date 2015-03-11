lwbd
====

*A lightweight beat detection library designed for simplicity and*
*portability*

What it does
------------
lwbd detects rhythmic onsets (beats) in digital audio. Feed it an audio file, and lwbd will provide you with the time location and sound energy of each beat it detects.

Platforms & Formats
-------------------
lwbd has two branches. The ```master``` branch is compatible with any
system providing a full JavaSE runtime. The ```android``` branch is
compatible with Android systems, which do not provide a full JavaSE
runtime. Due to the absence of the Java Sound API (javax.sound) in
Android, the vast majority of audio codecs written in Java will not
run on Android. As a result only a limited subset of the supported
formats are supported in the ```android``` branch due to a lack of
Android-compatible decoders.

For all formats, 44.1kHz is the only sample rate supported. lwbd assumes
all audio it receives is sampled at 44.1 kHz.

**Format availability by platform:**

| Format | Android | Desktop |
| :----- | :-----: | :-----: |
| MP3    | Yes     | Yes     |
| Vorbis | Yes     | Yes     |
| AAC    | No      | No      |
| FLAC   | No      | No      |
| WMA    | No      | No      |
| AIFF   | No      | No      |

Usage
-----
Here's an example of using one of the builtin decoders to perform
beat detection on an mp3 file:

```
File myAudioFile = new File("audio.mp3");
LwbdDecoder decoder = new JLayerMp3Decoder(myAudioFile);
BeatDetector.detectBeats(decoder, 1.4f);
```

Decoders
--------
Internally, lwbd operates only on PCM data. However, it is somewhat
unlikely that you will want to analyze .wav files. Much more often it
is the case that we want to analyze a file in one of the common consumer
formats such as MP3 or Ogg Vorbis.

To save you some trouble, lwbd provides baked-in decoders for
many common formats. However, not all formats are implemented, and
most of the baked-in decoders are unavailable on Android due to lack of
support for the Java Sound API. Therefore, lwbd provides a LwbdDecoder
interface which you can implement to add more decoders. You implement this
interface to support the audio format of your choosing, instantiate your
decoder with the audio you wish to analyze, and pass it to lwbd.
lwbd will then use your decoder to get the raw PCM data it needs.

LwbdDecoder defines one method signature:

```
public short[] nextMonoFrame();
```

When you implement your decoder class, all it is required to do is
return 1024 mono (only 1 channel, no interlacing) PCM samples per call
to this method. These 1024 samples together constitute one 'frame'.

Consecutive calls to this function should return consecutive frames.
That is, I should be able to get the entire audio file by calling
nextMonoFrame() over and over again until your decoder reaches the end
of the audio and runs out of frames. 

Odds are that the last frame won't be exactly 1024 samples. If you're
returning the last frame of audio, the size of the array should be
the amount of samples you're returning, not 1024. Returning less than
1024 samples tells lwbd that you've run out of audio.

If you need an example, feel free to read the source for any of the
builtin decoders.

Builtin Decoders
----------------
You get 3 decoders out of the box:
- GdxMp3Decoder
- GdxOggDecoder
- JLayerMp3Decoder

GdxMp3Decoder and GdxOggDecoder are useful if you are using lwbd inside
the game framework LibGDX. They take advantage of LibGDX's native
MP3 and Ogg Vorbis decoders to speed up audio analysis. In order to
use them you must have the gdx-audio extension added to your LibGDX
project.

JLayerMp3Decoder works with MP3 files. It uses JLayer to decode MP3.
Because of this, it runs everywhere. The disadvantage to this is
that JLayer is quite slow, especially on Android.

Technical
---------
lwbd is a pure Java implementation of Frédéric Patin's Frequency
Selected Sound Energy Algorithm #1 [found here](
http://www.flipcode.com/misc/BeatDetectionAlgorithms.pdf)

License
-------
```
Copyright (C) 2915  Quentin Young

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
