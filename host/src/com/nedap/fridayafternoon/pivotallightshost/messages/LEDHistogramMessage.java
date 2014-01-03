package com.nedap.fridayafternoon.pivotallightshost.messages;

import com.nedap.fridayafternoon.pivotallightshost.Client;
import com.nedap.fridayafternoon.pivotallightshost.Color;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public class LEDHistogramMessage extends ClientMessageGenerator {
    final int histogramParts;
    final Color[] histogramColors;
    final double[] histogramDivision;

    public LEDHistogramMessage(int histogramParts) {
        super(0x10);
        this.histogramParts = histogramParts;
        histogramColors = new Color[histogramParts];
        histogramDivision = new double[histogramParts-1];
    }

    public void setHistogramColor(int part, Color color) {
        histogramColors[part] = color;
    }

    public void setHistogramPartSize(int part, double fraction) {
        histogramDivision[part] = fraction;
    }

    @Override
    public int[] buildPayload(Client client) {
        int p = 0;
        int[] result = new int[1+3*histogramColors.length+histogramDivision.length];
        result[p++] = histogramParts;

        for (Color c : histogramColors) {
            if (c == null) {
                c = new Color(0,0,0);
            }
            result[p++] = c.r;
            result[p++] = c.g;
            result[p++] = c.b;
        }

        double cum = 0;
        for (double d : histogramDivision) {
            cum += d;
            result[p++] = (int)(cum*client.getLedCount());
        }

        return result;
    }
}
