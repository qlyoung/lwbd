package v4lk.lwbd.decoders.processing.jflac.apps;

/* libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2000,2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

import v4lk.lwbd.decoders.processing.jflac.FLACDecoder;
import v4lk.lwbd.decoders.processing.jflac.FrameListener;
import v4lk.lwbd.decoders.processing.jflac.frame.Frame;
import v4lk.lwbd.decoders.processing.jflac.metadata.Metadata;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Analyser reads all metadata and frame blocks in a FLAC file and outputs a text
 * representation of them.
 * @author kc7bfi
 */
public class Analyser implements FrameListener {
    private int frameNum = 0;
    
    /**
     * Analyse an input FLAC file.
     * @param inFileName The input file name
     * @throws java.io.IOException thrown if error reading file
     */
    public void analyse(String inFileName) throws IOException {
        System.out.println("FLAX Analysis for " + inFileName);
        FileInputStream is = new FileInputStream(inFileName);
        FLACDecoder decoder = new FLACDecoder(is);
        decoder.addFrameListener(this);
        decoder.decode();
    }
    
    /**
     * Process metadata records.
     * @param metadata the metadata block
     * @see v4lk.lwbd.decoders.processing.jflac.FrameListener#processMetadata(org.kc7bfi.jflac.metadata.MetadataBase)
     */
    public void processMetadata(Metadata metadata) {
        System.out.println(metadata.toString());
    }
    
    /**
     * Process data frames.
     * @param frame the data frame
     * @see v4lk.lwbd.decoders.processing.jflac.FrameListener#processFrame(v4lk.lwbd.decoders.processing.jflac.frame.Frame)
     */
    public void processFrame(Frame frame) {
        frameNum++;
        System.out.println(frameNum + " " + frame.toString());
    }
   
    /**
     * Called for each frame error detected.
     * @param msg   The error message
     * @see v4lk.lwbd.decoders.processing.jflac.FrameListener#processError(String)
     */
    public void processError(String msg) {
        System.out.println("Frame Error: " + msg);
    }
    
    /**
     * Main routine.
     * <p>args[0] is the FLAC file name to analyse
     * @param args  Command arguments
     */
    public static void main(String[] args) {
        try {
            Analyser analyser = new Analyser();
            analyser.analyse(args[0]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
