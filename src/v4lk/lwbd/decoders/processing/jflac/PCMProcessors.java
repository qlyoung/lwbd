/*
 * Created on Jun 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package v4lk.lwbd.decoders.processing.jflac;

import v4lk.lwbd.decoders.processing.jflac.metadata.StreamInfo;
import v4lk.lwbd.decoders.processing.jflac.util.ByteData;

import java.util.HashSet;
import java.util.Iterator;


/**
 * Class to handle PCM processors.
 * @author kc7bfi
 */
class PCMProcessors implements PCMProcessor {
    private HashSet pcmProcessors = new HashSet();
    
    /**
     * Add a PCM processor.
     * @param processor  The processor listener to add
     */
    public void addPCMProcessor(PCMProcessor processor) {
        synchronized (pcmProcessors) {
            pcmProcessors.add(processor);
        }
    }
     
    /**
     * Remove a PCM processor.
     * @param processor  The processor listener to remove
     */
    public void removePCMProcessor(PCMProcessor processor) {
        synchronized (pcmProcessors) {
            pcmProcessors.remove(processor);
        }
    }
    
    /**
     * Process the StreamInfo block.
     * @param info the StreamInfo block
     * @see PCMProcessor#processStreamInfo(v4lk.lwbd.decoders.processing.jflac.metadata.StreamInfo)
     */
    public void processStreamInfo(StreamInfo info) {
        synchronized (pcmProcessors) {
            Iterator it = pcmProcessors.iterator();
            while (it.hasNext()) {
                PCMProcessor processor = (PCMProcessor)it.next();
                processor.processStreamInfo(info);
            }
        }
    }
    
    /**
     * Process the decoded PCM bytes.
     * @param pcm The decoded PCM data
     * @see PCMProcessor#processPCM(org.kc7bfi.jflac.util.ByteSpace)
     */
    public void processPCM(ByteData pcm) {
        synchronized (pcmProcessors) {
            Iterator it = pcmProcessors.iterator();
            while (it.hasNext()) {
                PCMProcessor processor = (PCMProcessor)it.next();
                processor.processPCM(pcm);
            }
        }
    }

}
