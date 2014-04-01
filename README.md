lwbd
====

*A lightweight beat detection library designed for simplicity and*
*portability*

What it does
------------
lwbd detects rhythmic onsets (beats) in digital audio.

Formats supported:
- MP3
- Ogg Vorbis
- Theoretically any

Sample rates supported:
- 44.1 kHz

Platforms supported:
- Linux
- Windows
- OSX
- Android
- everywhere Java runs

How to Use
----------
Here's an example of using one of the premade decoders to decode an
mp3 file:

```
File myAudioFile = new File("audio.mp3");
LwbdDecoder decoder = new JLayerMp3Decoder(myAudioFile);
BeatDetector.detectBeats(decoder, 1.4f);
```

For more information about decoders, see 'Decoders'.

Decoders
--------
Internally, lwbd operates only on PCM data. However, it is somewhat
unlikely that you will want to analyze .wav files. It far more unlikely
that I feel like hardcoding support for MP3, Ogg Vorbis, FLAC, AIFF, WMA
 - whatever you have - into lwbd.
 
In order to provide a balance of convenience and usability, lwbd
supplies an interface called 'LwbdDecoder'. You implement this interface
to support the audio format of your choosing, initialize your
implementation with the audio you wish to analyze, and pass it to lwbd.
lwbd takes care of the rest.

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
three decoders that come with lwbd.

Premade Decoders
----------------------
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
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
```
