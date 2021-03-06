/*
 * ABCTranscriber.java
 *
 * Created on 06 July 2007, 12:10
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package matt;
import java.util.*;
/**
 *
 * @author Bryan
 */
public class ABCTranscriber {
    
    private static final float RANGE = 0.1f;    
    public static final float RATIO = 1.05946309436f;
    
    private TranscribedNote[] transcribedNotes;
    ODCFTranscriber transcriber;
    
    public static final int REEL = 0;
    public static final int JIG = 1;
    public static final int HORNPIPE = 2;
    public static final int MIDI_NOTES = 87;
    public static final int MIDI_START = 21;

    public String spell(float frequency)
    {
        float distance[] = new float[knownFrequencies.length];
        for (int j = 0 ; j < knownFrequencies.length ; j ++)
        {
            float difference = frequency - knownFrequencies[j];

            distance[j] = difference * difference;
        }
        int minIndex = 0;
        float min = Float.MAX_VALUE;
        for (int j = 0 ; j < knownFrequencies.length ; j ++)
        {
            if (distance[j] < min)
            {
                minIndex = j;
                min = distance[j];
            }
        }
        return noteNames[minIndex];
    }

    /**
     * @return the transcribedNotes
     */
    public TranscribedNote[] getTranscribedNotes()
    {
        return transcribedNotes;
    }

    /**
     * @param transcribedNotes the transcribedNotes to set
     */
    public void setTranscribedNotes(TranscribedNote[] transcribedNotes)
    {
        this.transcribedNotes = transcribedNotes;
    }

    
    enum pitch_model {FLUTE, WHISTLE};
    
    private pitch_model pitchModel;
    public static int[] NOTES_PER_BAR = { 8, 6, 8 };
    public static int[] NOTES_PER_PART = { 64, 48, 64 };
    private int tuneType = REEL;
           
    public static String [] noteNames = {"D,", "E,", "F,", "G,", "A,", "B,", "C", "C", "D", "E", "F", "G", "A", "B","c", "c", "d", "e", "f", "g", "a", "b", "c'", "c'", "d'", "e'", "f'", "g'", "a'", "b'", "c''", "c''", "d''"}; 
    public static float[] knownFrequencies = new float[noteNames.length];
    public static float[] midiNotes = new float[87];
    
    public static final int MIDI_OFFSET = 21;
    public int[] quantisedMidi;
    
    // public static final float D3 = 146.83f; 
    // public static final float D4=  293.66f; // Start transcription of the whistle one octive up
        
    private float maxEnergy;
    private float averageEnergy;
    
    public pitch_model getPitchModel()
    {
        return pitchModel;
    }

    public void setPitchModel(pitch_model pitchModel)
    {
        this.pitchModel = pitchModel;
    }
    
    void testScale()
    {
        for (int i = 0 ; i < knownFrequencies.length ; i ++)
        {
            System.out.print(noteNames[i] + "\t");
            TonePlayer.playTone(knownFrequencies[i], 0.25f, 0.1f);
        }
    }
    
    boolean isWholeToneInterval(int n, int[] intervals) 
    {
        n = n % 8;
        for (int i = 0 ; i < intervals.length; i ++)
        {
            if (n == intervals[i])
            {
                return true;
            }
        }
        return false;
    }

    void makeScale(String mode) 
    {
        
        // W - W - H - W - W - H - H - H
        int[] majorKeyIntervals = {1, 2, 4, 5};
        if (mode.equals("Major"))
        {
            if (pitchModel == pitch_model.FLUTE)
            {
                knownFrequencies[0] = MattProperties.getFloat(MattProperties.getString("fundamentalNote")) / (float) Math.pow(RATIO, 12);
            }
            else
            {   // Use the whistle pitch model
                knownFrequencies[0] = MattProperties.getFloat(MattProperties.getString("fundamentalNote"));
            }
            // W - W - H - W - W - W - H
            for (int i = 1 ; i < knownFrequencies.length ; i ++)
            {
               if (isWholeToneInterval(i, majorKeyIntervals))
               {
                       knownFrequencies[i] = knownFrequencies[i - 1] * RATIO * RATIO;
               }
               else
               {
                    knownFrequencies[i] = knownFrequencies[i - 1] * RATIO;
               }               
            }
        }
        
        System.out.println("FREQUENCIES:");
        for (int i = 0 ; i < knownFrequencies.length ; i ++)
        {
            System.out.println(noteNames[i] + "\t" + knownFrequencies[i]);
        }
        
    }
    private void makeMidiNotes()
    {
        midiNotes[0] = 27.5f;
        for (int i = 1 ; i < MIDI_NOTES ; i ++)
        {
            midiNotes[i] = midiNotes[i - 1] * RATIO;
        }
    }
            
    /** Creates a new instance of ABCTranscriber */
    public ABCTranscriber() {
        pitchModel = pitch_model.FLUTE;
        
    }
        
    public ABCTranscriber(ODCFTranscriber transcriber) {
        this.transcribedNotes = transcriber.getTranscribedNotes();
        this.transcriber = transcriber;
        makeMidiNotes();
    }
    
    public String convertToMidi()
    {   
        StringBuffer ret = new StringBuffer();
        for (int i = 0 ; i < getTranscribedNotes().length; i ++)
        {
            float distance[] = new float[MIDI_NOTES];

            for (int j = 0 ; j < MIDI_NOTES ; j ++)
            {
                float difference = getTranscribedNotes()[i].getFrequency() - midiNotes[j];

                distance[j] = difference * difference;
            }
            int minIndex = 0;
            float min = Float.MAX_VALUE; 
            for (int j = 0 ; j < MIDI_NOTES ; j ++)
            {
                if (distance[j] < min)
                {
                    minIndex = j;
                    min = distance[j];
                }
            }
            ret.append("" + (minIndex + MIDI_OFFSET));
            if (i < getTranscribedNotes().length - 1)
            {
                ret.append(",");
            }
            getTranscribedNotes()[i].setMidiNote(minIndex + MIDI_OFFSET);
        }
        return ret.toString();
    }
    
    public String convertToParsons()
    {
        convertToMidi();
        StringBuffer parsons = new StringBuffer();
        float previousNote = -1;
        for (int i = 0 ; i < getTranscribedNotes().length ; i ++)
        {
            float currentNote = getTranscribedNotes()[i].getMidiNote();
            // No parsons code for the first note
            if (previousNote != -1)
            {
                if (currentNote > previousNote)
                {
                    parsons.append("U");
                }
                else if (currentNote < previousNote)
                {
                    parsons.append("D");
                }
                else
                {
                    parsons.append("S");
                }
            }
            previousNote = currentNote;
        }
        return parsons.toString();
    }
    
    public String convertToABC()
    {
    	ArrayList<Integer> qMidi = new ArrayList<Integer>();
        calculatePitchModel();
        makeScale("Major");
        printScale();
        StringBuffer sb = new StringBuffer();
        convertToMidi();
        
        float standardNote = calculateStandardNoteDuration();
        EnergyCalculator ec = new EnergyCalculator();
        ec.setSignal(transcriber.getSignal());
        setMaxEnergy(ec.calculateMaxEnergy());
        setAverageEnergy(ec.calculateAverageEnergy());
        Logger.log("Max energy in signal: " + ec.formatEnergy(getMaxEnergy()));
        Logger.log("Average energy in signal: " + ec.formatEnergy(getAverageEnergy()));
        int quaverQ = 0;
        for (int i = 0 ; i < getTranscribedNotes().length ; i ++)
        {
            boolean found = false;
            getTranscribedNotes()[i].setQuaverQ(quaverQ);
            if (isBreath(getTranscribedNotes()[i]))
            {
                Logger.log("Breath detected at frame: " + i);
                getTranscribedNotes()[i].setSpelling("z");
                if (sb.length() != 0)
                {                    
                    found = true;
                }
                else
                {
                    Logger.log("Ignoring breath before opening note");
                }
            }
            else
            {
                String closest = spell(getTranscribedNotes()[i].getFrequency());
                found = true;
                getTranscribedNotes()[i].setSpelling(closest);
            }
            if (found)
            {
                
                if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) == 'z') && (getTranscribedNotes()[i].getSpelling().equals("z")))
                {
                    continue;
                }
                 
                sb.append(getTranscribedNotes()[i].getSpelling());
                qMidi.add(getTranscribedNotes()[i].getMidiNote());
                // A breath should never be longer than a single note
                int nearestMultiple = 0;
                if (!transcribedNotes[i].getSpelling().equals("z"))
                {
                    //nearestMultiple = OnsetPostProcessor.calculateNearestMultiple(transcribedNotes[i].getDuration(), standardNote);
                    nearestMultiple = getTranscribedNotes()[i].getMultiple();
                    if (nearestMultiple > 1)
                    {
                        // Quantise at dottet crochets 
                        if (nearestMultiple > 3)
                        {
                            Logger.log("Note of length " + nearestMultiple + " detected. Quantising at 3");
                            nearestMultiple = 3;
                        }
                        sb.append("" + nearestMultiple);
                        
                    }
                }
                else
                {
                    nearestMultiple = 1;
                }
                

                quaverQ += nearestMultiple;
                getTranscribedNotes()[i].setMultiple(nearestMultiple);
                if (quaverQ % NOTES_PER_BAR[tuneType] == 0)
                {
                    // sb.append("|" + System.getProperty("line.separator"));
                }
            }
            else
            {
                Logger.log("Ignoring: " + getTranscribedNotes()[i]);
            }
        }
        // Now remove z's at the end
        while (sb.charAt(sb.length() -1) == 'z')
        {
            sb.setLength(sb.length()-1);
        }
        
		quantisedMidi = new int[qMidi.size()];
	    Iterator<Integer> iterator = qMidi.iterator();
	    for (int i = 0; i < quantisedMidi.length; i++)
	    {
	    	quantisedMidi[i] = iterator.next().intValue();
	    }        
        return sb.toString();
    }
    
    private boolean isBreath(TranscribedNote note)
    {
        float threshold = averageEnergy * Float.parseFloat("" + MattProperties.instance().get("breathThreshold"));
        
        if (note.getEnergy() < threshold)
        {
           Logger.log(note.getEnergy()  + " energy is less than the threshold of " + threshold);
           return true;
        }
        if (note.getFrequency() < 100.0f)
        {
            Logger.log(note.getFrequency() + " frequency is less than 100hz");
           return true;
        }
        
        return false;
    }

    public int getTuneType() {
        return tuneType;
    }

    public void setTuneType(int tuneType) {
        this.tuneType = tuneType;
    }
    
    public void printScale()
    {
        System.out.println("SCALE:");
        for (int i = 0 ; i < noteNames.length; i ++)
        {
            Logger.log(noteNames[i] + ": " + knownFrequencies[i]);
        }
    }
    
    float calculateStandardNoteDuration()
    {      
        float duration = 0.0f;
        float[] histData = new float[getTranscribedNotes().length];
        FuzzyHistogram fuzzyHistogram = new FuzzyHistogram();
        
        for (int i = 0 ; i < getTranscribedNotes().length ; i ++)
        {
            histData[i] = getTranscribedNotes()[i].getDuration();
        }
        duration = fuzzyHistogram.calculatePeek(histData, 0.3f);
        return duration;
    }
    
    public float getMaxEnergy()
    {
        return maxEnergy;
    }

    public void setMaxEnergy(float maxEnergy)
    {
        this.maxEnergy = maxEnergy;
    }

    public float getAverageEnergy()
    {
        return averageEnergy;
    }

    public void setAverageEnergy(float averageEnergy)
    {
        this.averageEnergy = averageEnergy;
    }
    
    public void calculatePitchModel()
    {
        // If the number of notes > half way through D5 is greater than the number of notes < half way through D5
        // Then its probably a tin whistle
        int flute = 0, whistle = 0;
        float G5 = (float) (MattProperties.getFloat(MattProperties.getString("fundamentalNote")) * Math.pow(RATIO, 17.0f));
        for (int i = 0 ; i < getTranscribedNotes().length ; i ++)
        {
            if (isBreath(getTranscribedNotes()[i]))
            {
                continue;
            }
            if (getTranscribedNotes()[i].getFrequency() < G5)
            {
                flute ++;
            }
            else
            {
                whistle ++;
            }
        }
        
        pitchModel = (flute >= whistle) ? pitch_model.FLUTE : pitch_model.WHISTLE;
        Logger.log("Using " + ((pitchModel == pitch_model.FLUTE) ? "flute " : "whistle ") + " pitch model");
    }
    
    public static void main(String[] args)
    {
        ABCTranscriber t = new ABCTranscriber();
        t.makeScale("Major");
        t.testScale();
    }
    
}