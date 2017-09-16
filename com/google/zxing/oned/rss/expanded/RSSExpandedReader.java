package com.google.zxing.oned.rss.expanded;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.oned.OneDReader;
import com.google.zxing.oned.rss.AbstractRSSReader;
import com.google.zxing.oned.rss.DataCharacter;
import com.google.zxing.oned.rss.FinderPattern;
import com.google.zxing.oned.rss.RSSUtils;
import com.google.zxing.oned.rss.expanded.decoders.AbstractExpandedDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class RSSExpandedReader extends AbstractRSSReader {
    private static final int[] EVEN_TOTAL_SUBSET = new int[]{4, 20, 52, 104, 204};
    private static final int[][] FINDER_PATTERNS;
    private static final int[][] FINDER_PATTERN_SEQUENCES;
    private static final int[] GSUM;
    private static final int[] SYMBOL_WIDEST = new int[]{7, 5, 4, 3, 1};
    private static final int[][] WEIGHTS;
    private final List<ExpandedPair> pairs = new ArrayList(11);
    private final List<ExpandedRow> rows = new ArrayList();
    private final int[] startEnd = new int[2];
    private boolean startFromEven = false;

    static {
        int[] iArr = new int[5];
        iArr[1] = 348;
        iArr[2] = 1388;
        iArr[3] = 2948;
        iArr[4] = 3988;
        GSUM = iArr;
        r0 = new int[6][];
        r0[0] = new int[]{1, 8, 4, 1};
        r0[1] = new int[]{3, 6, 4, 1};
        r0[2] = new int[]{3, 4, 6, 1};
        r0[3] = new int[]{3, 2, 8, 1};
        r0[4] = new int[]{2, 6, 5, 1};
        r0[5] = new int[]{2, 2, 9, 1};
        FINDER_PATTERNS = r0;
        r0 = new int[23][];
        r0[0] = new int[]{1, 3, 9, 27, 81, 32, 96, 77};
        r0[1] = new int[]{20, 60, 180, 118, 143, 7, 21, 63};
        r0[2] = new int[]{189, 145, 13, 39, 117, 140, 209, 205};
        r0[3] = new int[]{193, 157, 49, 147, 19, 57, 171, 91};
        r0[4] = new int[]{62, 186, 136, 197, 169, 85, 44, 132};
        r0[5] = new int[]{185, 133, 188, 142, 4, 12, 36, 108};
        r0[6] = new int[]{113, 128, 173, 97, 80, 29, 87, 50};
        r0[7] = new int[]{150, 28, 84, 41, 123, 158, 52, 156};
        r0[8] = new int[]{46, 138, 203, 187, 139, 206, 196, 166};
        r0[9] = new int[]{76, 17, 51, 153, 37, 111, 122, 155};
        r0[10] = new int[]{43, 129, 176, 106, 107, 110, 119, 146};
        r0[11] = new int[]{16, 48, 144, 10, 30, 90, 59, 177};
        r0[12] = new int[]{109, 116, 137, 200, 178, 112, 125, 164};
        r0[13] = new int[]{70, 210, 208, 202, 184, 130, 179, 115};
        r0[14] = new int[]{134, 191, 151, 31, 93, 68, 204, 190};
        r0[15] = new int[]{148, 22, 66, 198, 172, 94, 71, 2};
        r0[16] = new int[]{6, 18, 54, 162, 64, 192, 154, 40};
        r0[17] = new int[]{120, 149, 25, 75, 14, 42, 126, 167};
        r0[18] = new int[]{79, 26, 78, 23, 69, 207, 199, 175};
        r0[19] = new int[]{103, 98, 83, 38, 114, 131, 182, 124};
        r0[20] = new int[]{161, 61, 183, 127, 170, 88, 53, 159};
        r0[21] = new int[]{55, 165, 73, 8, 24, 72, 5, 15};
        r0[22] = new int[]{45, 135, 194, 160, 58, 174, 100, 89};
        WEIGHTS = r0;
        r0 = new int[10][];
        int[] iArr2 = new int[]{1, 1, iArr2};
        iArr2 = new int[]{2, 1, 3, iArr2};
        iArr2 = new int[]{4, 1, 3, 2, iArr2};
        iArr2 = new int[]{4, 1, 3, 3, 5, iArr2};
        iArr2 = new int[]{4, 1, 3, 4, 5, 5, iArr2};
        int[] iArr3 = new int[8];
        iArr3[2] = 1;
        iArr3[3] = 1;
        iArr3[4] = 2;
        iArr3[5] = 2;
        iArr3[6] = 3;
        iArr3[7] = 3;
        r0[6] = iArr3;
        iArr3 = new int[9];
        iArr3[2] = 1;
        iArr3[3] = 1;
        iArr3[4] = 2;
        iArr3[5] = 2;
        iArr3[6] = 3;
        iArr3[7] = 4;
        iArr3[8] = 4;
        r0[7] = iArr3;
        iArr3 = new int[10];
        iArr3[2] = 1;
        iArr3[3] = 1;
        iArr3[4] = 2;
        iArr3[5] = 2;
        iArr3[6] = 3;
        iArr3[7] = 4;
        iArr3[8] = 5;
        iArr3[9] = 5;
        r0[8] = iArr3;
        iArr3 = new int[11];
        iArr3[2] = 1;
        iArr3[3] = 1;
        iArr3[4] = 2;
        iArr3[5] = 3;
        iArr3[6] = 3;
        iArr3[7] = 4;
        iArr3[8] = 4;
        iArr3[9] = 5;
        iArr3[10] = 5;
        r0[9] = iArr3;
        FINDER_PATTERN_SEQUENCES = r0;
    }

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> map) throws NotFoundException, FormatException {
        this.pairs.clear();
        this.startFromEven = false;
        try {
            return constructResult(decodeRow2pairs(rowNumber, row));
        } catch (NotFoundException e) {
            this.pairs.clear();
            this.startFromEven = true;
            return constructResult(decodeRow2pairs(rowNumber, row));
        }
    }

    public void reset() {
        this.pairs.clear();
        this.rows.clear();
    }

    List<ExpandedPair> decodeRow2pairs(int rowNumber, BitArray row) throws NotFoundException {
        while (true) {
            try {
                this.pairs.add(retrieveNextPair(row, this.pairs, rowNumber));
            } catch (NotFoundException nfe) {
                if (this.pairs.isEmpty()) {
                    throw nfe;
                } else if (checkChecksum()) {
                    return this.pairs;
                } else {
                    boolean tryStackedDecode;
                    if (this.rows.isEmpty()) {
                        tryStackedDecode = false;
                    } else {
                        tryStackedDecode = true;
                    }
                    storeRow(rowNumber, false);
                    if (tryStackedDecode) {
                        List<ExpandedPair> ps = checkRows(false);
                        if (ps != null) {
                            return ps;
                        }
                        ps = checkRows(true);
                        if (ps != null) {
                            return ps;
                        }
                    }
                    throw NotFoundException.getNotFoundInstance();
                }
            }
        }
    }

    private List<ExpandedPair> checkRows(boolean reverse) {
        if (this.rows.size() <= 25) {
            this.pairs.clear();
            if (reverse) {
                Collections.reverse(this.rows);
            }
            List<ExpandedPair> ps = null;
            try {
                ps = checkRows(new ArrayList(), 0);
            } catch (NotFoundException e) {
            }
            if (reverse) {
                Collections.reverse(this.rows);
            }
            return ps;
        }
        this.rows.clear();
        return null;
    }

    private List<ExpandedPair> checkRows(List<ExpandedRow> collectedRows, int currentRow) throws NotFoundException {
        int i = currentRow;
        while (i < this.rows.size()) {
            ExpandedRow row = (ExpandedRow) this.rows.get(i);
            this.pairs.clear();
            int size = collectedRows.size();
            for (int j = 0; j < size; j++) {
                this.pairs.addAll(((ExpandedRow) collectedRows.get(j)).getPairs());
            }
            this.pairs.addAll(row.getPairs());
            if (!isValidSequence(this.pairs)) {
                i++;
            } else if (checkChecksum()) {
                return this.pairs;
            } else {
                List<ExpandedRow> rs = new ArrayList();
                rs.addAll(collectedRows);
                rs.add(row);
                try {
                    return checkRows(rs, i + 1);
                } catch (NotFoundException e) {
                }
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static boolean isValidSequence(List<ExpandedPair> pairs) {
        for (int[] sequence : FINDER_PATTERN_SEQUENCES) {
            if (pairs.size() <= sequence.length) {
                boolean stop = true;
                for (int j = 0; j < pairs.size(); j++) {
                    if (((ExpandedPair) pairs.get(j)).getFinderPattern().getValue() != sequence[j]) {
                        stop = false;
                        break;
                    }
                }
                if (stop) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void storeRow(int r8, boolean r9) {
        /*
        r7 = this;
        r1 = 0;
        r3 = 0;
        r2 = 0;
    L_0x0003:
        r4 = r7.rows;
        r4 = r4.size();
        if (r1 >= r4) goto L_0x0028;
    L_0x000b:
        r4 = r7.rows;
        r0 = r4.get(r1);
        r0 = (com.google.zxing.oned.rss.expanded.ExpandedRow) r0;
        r4 = r0.getRowNumber();
        if (r4 > r8) goto L_0x0022;
    L_0x0019:
        r4 = r7.pairs;
        r3 = r0.isEquivalent(r4);
        r1 = r1 + 1;
        goto L_0x0003;
    L_0x0022:
        r4 = r7.pairs;
        r2 = r0.isEquivalent(r4);
    L_0x0028:
        if (r2 == 0) goto L_0x002b;
    L_0x002a:
        return;
    L_0x002b:
        if (r3 != 0) goto L_0x002a;
    L_0x002d:
        r4 = r7.pairs;
        r5 = r7.rows;
        r4 = isPartialRow(r4, r5);
        if (r4 != 0) goto L_0x004b;
    L_0x0037:
        r4 = r7.rows;
        r5 = new com.google.zxing.oned.rss.expanded.ExpandedRow;
        r6 = r7.pairs;
        r5.<init>(r6, r8, r9);
        r4.add(r1, r5);
        r4 = r7.pairs;
        r5 = r7.rows;
        removePartialRows(r4, r5);
        return;
    L_0x004b:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.zxing.oned.rss.expanded.RSSExpandedReader.storeRow(int, boolean):void");
    }

    private static void removePartialRows(List<ExpandedPair> pairs, List<ExpandedRow> rows) {
        Iterator<ExpandedRow> iterator = rows.iterator();
        while (iterator.hasNext()) {
            ExpandedRow r = (ExpandedRow) iterator.next();
            if (r.getPairs().size() != pairs.size()) {
                boolean allFound = true;
                for (ExpandedPair p : r.getPairs()) {
                    boolean found = false;
                    for (ExpandedPair pp : pairs) {
                        if (p.equals(pp)) {
                            found = true;
                            continue;
                            break;
                        }
                    }
                    if (!found) {
                        allFound = false;
                        break;
                    }
                }
                if (allFound) {
                    iterator.remove();
                }
            }
        }
    }

    private static boolean isPartialRow(Iterable<ExpandedPair> pairs, Iterable<ExpandedRow> rows) {
        for (ExpandedRow r : rows) {
            boolean allFound = true;
            for (ExpandedPair p : pairs) {
                boolean found = false;
                for (ExpandedPair pp : r.getPairs()) {
                    if (p.equals(pp)) {
                        found = true;
                        continue;
                        break;
                    }
                }
                if (!found) {
                    allFound = false;
                    continue;
                    break;
                }
            }
            if (allFound) {
                return true;
            }
        }
        return false;
    }

    static Result constructResult(List<ExpandedPair> pairs) throws NotFoundException, FormatException {
        String resultingString = AbstractExpandedDecoder.createDecoder(BitArrayBuilder.buildBitArray(pairs)).parseInformation();
        ResultPoint[] firstPoints = ((ExpandedPair) pairs.get(0)).getFinderPattern().getResultPoints();
        ResultPoint[] lastPoints = ((ExpandedPair) pairs.get(pairs.size() - 1)).getFinderPattern().getResultPoints();
        return new Result(resultingString, null, new ResultPoint[]{firstPoints[0], firstPoints[1], lastPoints[0], lastPoints[1]}, BarcodeFormat.RSS_EXPANDED);
    }

    private boolean checkChecksum() {
        ExpandedPair firstPair = (ExpandedPair) this.pairs.get(0);
        DataCharacter checkCharacter = firstPair.getLeftChar();
        DataCharacter firstCharacter = firstPair.getRightChar();
        if (firstCharacter == null) {
            return false;
        }
        int checksum = firstCharacter.getChecksumPortion();
        int s = 2;
        for (int i = 1; i < this.pairs.size(); i++) {
            ExpandedPair currentPair = (ExpandedPair) this.pairs.get(i);
            checksum += currentPair.getLeftChar().getChecksumPortion();
            s++;
            DataCharacter currentRightChar = currentPair.getRightChar();
            if (currentRightChar != null) {
                checksum += currentRightChar.getChecksumPortion();
                s++;
            }
        }
        if (((s - 4) * 211) + (checksum % 211) != checkCharacter.getValue()) {
            return false;
        }
        return true;
    }

    private static int getNextSecondBar(BitArray row, int initialPos) {
        if (row.get(initialPos)) {
            return row.getNextSet(row.getNextUnset(initialPos));
        }
        return row.getNextUnset(row.getNextSet(initialPos));
    }

    ExpandedPair retrieveNextPair(BitArray row, List<ExpandedPair> previousPairs, int rowNumber) throws NotFoundException {
        boolean isOddPattern;
        FinderPattern pattern;
        if (previousPairs.size() % 2 != 0) {
            isOddPattern = false;
        } else {
            isOddPattern = true;
        }
        if (this.startFromEven) {
            isOddPattern = !isOddPattern;
        }
        boolean keepFinding = true;
        int forcedOffset = -1;
        do {
            findNextPair(row, previousPairs, forcedOffset);
            pattern = parseFoundFinderPattern(row, rowNumber, isOddPattern);
            if (pattern != null) {
                keepFinding = false;
                continue;
            } else {
                forcedOffset = getNextSecondBar(row, this.startEnd[0]);
                continue;
            }
        } while (keepFinding);
        DataCharacter leftChar = decodeDataCharacter(row, pattern, isOddPattern, true);
        if (!previousPairs.isEmpty() && ((ExpandedPair) previousPairs.get(previousPairs.size() - 1)).mustBeLast()) {
            throw NotFoundException.getNotFoundInstance();
        }
        DataCharacter decodeDataCharacter;
        try {
            decodeDataCharacter = decodeDataCharacter(row, pattern, isOddPattern, false);
        } catch (NotFoundException e) {
            decodeDataCharacter = null;
        }
        return new ExpandedPair(leftChar, decodeDataCharacter, pattern, true);
    }

    private void findNextPair(BitArray row, List<ExpandedPair> previousPairs, int forcedOffset) throws NotFoundException {
        boolean searchingEvenPair;
        int[] counters = getDecodeFinderCounters();
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        int width = row.getSize();
        int rowOffset = forcedOffset < 0 ? !previousPairs.isEmpty() ? ((ExpandedPair) previousPairs.get(previousPairs.size() - 1)).getFinderPattern().getStartEnd()[1] : 0 : forcedOffset;
        if (previousPairs.size() % 2 == 0) {
            searchingEvenPair = false;
        } else {
            searchingEvenPair = true;
        }
        if (this.startFromEven) {
            searchingEvenPair = !searchingEvenPair;
        }
        int i = 0;
        while (rowOffset < width) {
            if (row.get(rowOffset)) {
                i = 0;
            } else {
                i = 1;
            }
            if (i == 0) {
                break;
            }
            rowOffset++;
        }
        int counterPosition = 0;
        int patternStart = rowOffset;
        for (int x = rowOffset; x < width; x++) {
            if ((row.get(x) ^ i) == 0) {
                if (counterPosition != 3) {
                    counterPosition++;
                } else {
                    if (searchingEvenPair) {
                        reverseCounters(counters);
                    }
                    if (AbstractRSSReader.isFinderPattern(counters)) {
                        this.startEnd[0] = patternStart;
                        this.startEnd[1] = x;
                        return;
                    }
                    if (searchingEvenPair) {
                        reverseCounters(counters);
                    }
                    patternStart += counters[0] + counters[1];
                    counters[0] = counters[2];
                    counters[1] = counters[3];
                    counters[2] = 0;
                    counters[3] = 0;
                    counterPosition--;
                }
                counters[counterPosition] = 1;
                if (i == 0) {
                    i = 1;
                } else {
                    i = 0;
                }
            } else {
                counters[counterPosition] = counters[counterPosition] + 1;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static void reverseCounters(int[] counters) {
        int length = counters.length;
        for (int i = 0; i < length / 2; i++) {
            int tmp = counters[i];
            counters[i] = counters[(length - i) - 1];
            counters[(length - i) - 1] = tmp;
        }
    }

    private FinderPattern parseFoundFinderPattern(BitArray row, int rowNumber, boolean oddPattern) {
        int firstCounter;
        int start;
        int end;
        if (oddPattern) {
            int firstElementStart = this.startEnd[0] - 1;
            while (firstElementStart >= 0 && !row.get(firstElementStart)) {
                firstElementStart--;
            }
            firstElementStart++;
            firstCounter = this.startEnd[0] - firstElementStart;
            start = firstElementStart;
            end = this.startEnd[1];
        } else {
            start = this.startEnd[0];
            end = row.getNextUnset(this.startEnd[1] + 1);
            firstCounter = end - this.startEnd[1];
        }
        int[] counters = getDecodeFinderCounters();
        System.arraycopy(counters, 0, counters, 1, counters.length - 1);
        counters[0] = firstCounter;
        try {
            return new FinderPattern(AbstractRSSReader.parseFinderValue(counters, FINDER_PATTERNS), new int[]{start, end}, start, end, rowNumber);
        } catch (NotFoundException e) {
            return null;
        }
    }

    DataCharacter decodeDataCharacter(BitArray row, FinderPattern pattern, boolean isOddPattern, boolean leftChar) throws NotFoundException {
        int i;
        int[] counters = getDataCharacterCounters();
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        counters[4] = 0;
        counters[5] = 0;
        counters[6] = 0;
        counters[7] = 0;
        if (leftChar) {
            OneDReader.recordPatternInReverse(row, pattern.getStartEnd()[0], counters);
        } else {
            OneDReader.recordPattern(row, pattern.getStartEnd()[1], counters);
            i = 0;
            for (int j = counters.length - 1; i < j; j--) {
                int temp = counters[i];
                counters[i] = counters[j];
                counters[j] = temp;
                i++;
            }
        }
        float elementWidth = ((float) AbstractRSSReader.count(counters)) / 17.0f;
        float expectedElementWidth = ((float) (pattern.getStartEnd()[1] - pattern.getStartEnd()[0])) / 15.0f;
        if (Math.abs(elementWidth - expectedElementWidth) / expectedElementWidth > 0.3f) {
            throw NotFoundException.getNotFoundInstance();
        }
        int i2;
        int[] oddCounts = getOddCounts();
        int[] evenCounts = getEvenCounts();
        float[] oddRoundingErrors = getOddRoundingErrors();
        float[] evenRoundingErrors = getEvenRoundingErrors();
        for (i = 0; i < counters.length; i++) {
            float value = (((float) counters[i]) * 1.0f) / elementWidth;
            int count = (int) (0.5f + value);
            if (count >= 1) {
                if (count > 8) {
                    if (value > 8.7f) {
                        throw NotFoundException.getNotFoundInstance();
                    }
                    count = 8;
                }
            } else if (value < 0.3f) {
                throw NotFoundException.getNotFoundInstance();
            } else {
                count = 1;
            }
            int offset = i / 2;
            if ((i & 1) != 0) {
                evenCounts[offset] = count;
                evenRoundingErrors[offset] = value - ((float) count);
            } else {
                oddCounts[offset] = count;
                oddRoundingErrors[offset] = value - ((float) count);
            }
        }
        adjustOddEvenCounts(17);
        int value2 = pattern.getValue() * 4;
        if (isOddPattern) {
            i2 = 0;
        } else {
            i2 = 2;
        }
        int weightRowNumber = ((!leftChar ? 1 : 0) + (value2 + i2)) - 1;
        int oddSum = 0;
        int oddChecksumPortion = 0;
        for (i = oddCounts.length - 1; i >= 0; i--) {
            if (isNotA1left(pattern, isOddPattern, leftChar)) {
                oddChecksumPortion += oddCounts[i] * WEIGHTS[weightRowNumber][i * 2];
            }
            oddSum += oddCounts[i];
        }
        int evenChecksumPortion = 0;
        for (i = evenCounts.length - 1; i >= 0; i--) {
            if (isNotA1left(pattern, isOddPattern, leftChar)) {
                evenChecksumPortion += evenCounts[i] * WEIGHTS[weightRowNumber][(i * 2) + 1];
            }
        }
        int checksumPortion = oddChecksumPortion + evenChecksumPortion;
        if ((oddSum & 1) == 0 && oddSum <= 13 && oddSum >= 4) {
            int group = (13 - oddSum) / 2;
            int oddWidest = SYMBOL_WIDEST[group];
            int evenWidest = 9 - oddWidest;
            int vOdd = RSSUtils.getRSSvalue(oddCounts, oddWidest, true);
            return new DataCharacter(((vOdd * EVEN_TOTAL_SUBSET[group]) + RSSUtils.getRSSvalue(evenCounts, evenWidest, false)) + GSUM[group], checksumPortion);
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static boolean isNotA1left(FinderPattern pattern, boolean isOddPattern, boolean leftChar) {
        if (pattern.getValue() == 0 && isOddPattern) {
            if (leftChar) {
                return false;
            }
        }
        return true;
    }

    private void adjustOddEvenCounts(int numModules) throws NotFoundException {
        boolean oddParityBad;
        boolean evenParityBad;
        int oddSum = AbstractRSSReader.count(getOddCounts());
        int evenSum = AbstractRSSReader.count(getEvenCounts());
        int mismatch = (oddSum + evenSum) - numModules;
        if ((oddSum & 1) != 1) {
            oddParityBad = false;
        } else {
            oddParityBad = true;
        }
        if ((evenSum & 1) != 0) {
            evenParityBad = false;
        } else {
            evenParityBad = true;
        }
        boolean incrementOdd = false;
        boolean decrementOdd = false;
        if (oddSum > 13) {
            decrementOdd = true;
        } else if (oddSum < 4) {
            incrementOdd = true;
        }
        boolean incrementEven = false;
        boolean decrementEven = false;
        if (evenSum > 13) {
            decrementEven = true;
        } else if (evenSum < 4) {
            incrementEven = true;
        }
        if (mismatch != 1) {
            if (mismatch != -1) {
                if (mismatch != 0) {
                    throw NotFoundException.getNotFoundInstance();
                } else if (oddParityBad) {
                    if (!evenParityBad) {
                        throw NotFoundException.getNotFoundInstance();
                    } else if (oddSum >= evenSum) {
                        decrementOdd = true;
                        incrementEven = true;
                    } else {
                        incrementOdd = true;
                        decrementEven = true;
                    }
                } else if (evenParityBad) {
                    throw NotFoundException.getNotFoundInstance();
                }
            } else if (oddParityBad) {
                if (evenParityBad) {
                    throw NotFoundException.getNotFoundInstance();
                }
                incrementOdd = true;
            } else if (evenParityBad) {
                incrementEven = true;
            } else {
                throw NotFoundException.getNotFoundInstance();
            }
        } else if (oddParityBad) {
            if (evenParityBad) {
                throw NotFoundException.getNotFoundInstance();
            }
            decrementOdd = true;
        } else if (evenParityBad) {
            decrementEven = true;
        } else {
            throw NotFoundException.getNotFoundInstance();
        }
        if (incrementOdd) {
            if (decrementOdd) {
                throw NotFoundException.getNotFoundInstance();
            }
            AbstractRSSReader.increment(getOddCounts(), getOddRoundingErrors());
        }
        if (decrementOdd) {
            AbstractRSSReader.decrement(getOddCounts(), getOddRoundingErrors());
        }
        if (incrementEven) {
            if (decrementEven) {
                throw NotFoundException.getNotFoundInstance();
            }
            AbstractRSSReader.increment(getEvenCounts(), getOddRoundingErrors());
        }
        if (decrementEven) {
            AbstractRSSReader.decrement(getEvenCounts(), getEvenRoundingErrors());
        }
    }
}
