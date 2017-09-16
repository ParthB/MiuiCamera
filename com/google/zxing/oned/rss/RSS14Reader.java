package com.google.zxing.oned.rss;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.BitArray;
import com.google.zxing.oned.OneDReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class RSS14Reader extends AbstractRSSReader {
    private static final int[][] FINDER_PATTERNS;
    private static final int[] INSIDE_GSUM;
    private static final int[] INSIDE_ODD_TOTAL_SUBSET = new int[]{4, 20, 48, 81};
    private static final int[] INSIDE_ODD_WIDEST = new int[]{2, 4, 6, 8};
    private static final int[] OUTSIDE_EVEN_TOTAL_SUBSET = new int[]{1, 10, 34, 70, 126};
    private static final int[] OUTSIDE_GSUM;
    private static final int[] OUTSIDE_ODD_WIDEST = new int[]{8, 6, 4, 3, 1};
    private final List<Pair> possibleLeftPairs = new ArrayList();
    private final List<Pair> possibleRightPairs = new ArrayList();

    static {
        int[] iArr = new int[5];
        iArr[1] = 161;
        iArr[2] = 961;
        iArr[3] = 2015;
        iArr[4] = 2715;
        OUTSIDE_GSUM = iArr;
        iArr = new int[4];
        iArr[1] = 336;
        iArr[2] = 1036;
        iArr[3] = 1516;
        INSIDE_GSUM = iArr;
        r0 = new int[9][];
        r0[0] = new int[]{3, 8, 2, 1};
        r0[1] = new int[]{3, 5, 5, 1};
        r0[2] = new int[]{3, 3, 7, 1};
        r0[3] = new int[]{3, 1, 9, 1};
        r0[4] = new int[]{2, 7, 4, 1};
        r0[5] = new int[]{2, 5, 6, 1};
        r0[6] = new int[]{2, 3, 8, 1};
        r0[7] = new int[]{1, 5, 7, 1};
        r0[8] = new int[]{1, 3, 9, 1};
        FINDER_PATTERNS = r0;
    }

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException {
        addOrTally(this.possibleLeftPairs, decodePair(row, false, rowNumber, hints));
        row.reverse();
        addOrTally(this.possibleRightPairs, decodePair(row, true, rowNumber, hints));
        row.reverse();
        int lefSize = this.possibleLeftPairs.size();
        for (int i = 0; i < lefSize; i++) {
            Pair left = (Pair) this.possibleLeftPairs.get(i);
            if (left.getCount() > 1) {
                int rightSize = this.possibleRightPairs.size();
                for (int j = 0; j < rightSize; j++) {
                    Pair right = (Pair) this.possibleRightPairs.get(j);
                    if (right.getCount() > 1 && checkChecksum(left, right)) {
                        return constructResult(left, right);
                    }
                }
                continue;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static void addOrTally(Collection<Pair> possiblePairs, Pair pair) {
        if (pair != null) {
            boolean found = false;
            for (Pair other : possiblePairs) {
                if (other.getValue() == pair.getValue()) {
                    other.incrementCount();
                    found = true;
                    break;
                }
            }
            if (!found) {
                possiblePairs.add(pair);
            }
        }
    }

    public void reset() {
        this.possibleLeftPairs.clear();
        this.possibleRightPairs.clear();
    }

    private static Result constructResult(Pair leftPair, Pair rightPair) {
        int i;
        String text = String.valueOf((((long) leftPair.getValue()) * 4537077) + ((long) rightPair.getValue()));
        StringBuilder buffer = new StringBuilder(14);
        for (i = 13 - text.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(text);
        int checkDigit = 0;
        for (i = 0; i < 13; i++) {
            int digit = buffer.charAt(i) - 48;
            if ((i & 1) == 0) {
                digit *= 3;
            }
            checkDigit += digit;
        }
        checkDigit = 10 - (checkDigit % 10);
        if (checkDigit == 10) {
            checkDigit = 0;
        }
        buffer.append(checkDigit);
        ResultPoint[] leftPoints = leftPair.getFinderPattern().getResultPoints();
        ResultPoint[] rightPoints = rightPair.getFinderPattern().getResultPoints();
        return new Result(String.valueOf(buffer.toString()), null, new ResultPoint[]{leftPoints[0], leftPoints[1], rightPoints[0], rightPoints[1]}, BarcodeFormat.RSS_14);
    }

    private static boolean checkChecksum(Pair leftPair, Pair rightPair) {
        int checkValue = (leftPair.getChecksumPortion() + (rightPair.getChecksumPortion() * 16)) % 79;
        int targetCheckValue = (leftPair.getFinderPattern().getValue() * 9) + rightPair.getFinderPattern().getValue();
        if (targetCheckValue > 72) {
            targetCheckValue--;
        }
        if (targetCheckValue > 8) {
            targetCheckValue--;
        }
        if (checkValue != targetCheckValue) {
            return false;
        }
        return true;
    }

    private Pair decodePair(BitArray row, boolean right, int rowNumber, Map<DecodeHintType, ?> hints) {
        try {
            ResultPointCallback resultPointCallback;
            int[] startEnd = findFinderPattern(row, 0, right);
            FinderPattern pattern = parseFoundFinderPattern(row, rowNumber, right, startEnd);
            if (hints != null) {
                resultPointCallback = (ResultPointCallback) hints.get(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
            } else {
                resultPointCallback = null;
            }
            if (resultPointCallback != null) {
                float center = ((float) (startEnd[0] + startEnd[1])) / 2.0f;
                if (right) {
                    center = ((float) (row.getSize() - 1)) - center;
                }
                resultPointCallback.foundPossibleResultPoint(new ResultPoint(center, (float) rowNumber));
            }
            DataCharacter outside = decodeDataCharacter(row, pattern, true);
            DataCharacter inside = decodeDataCharacter(row, pattern, false);
            return new Pair((outside.getValue() * 1597) + inside.getValue(), outside.getChecksumPortion() + (inside.getChecksumPortion() * 4), pattern);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private DataCharacter decodeDataCharacter(BitArray row, FinderPattern pattern, boolean outsideChar) throws NotFoundException {
        int i;
        int numModules;
        int[] counters = getDataCharacterCounters();
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        counters[4] = 0;
        counters[5] = 0;
        counters[6] = 0;
        counters[7] = 0;
        if (outsideChar) {
            OneDReader.recordPatternInReverse(row, pattern.getStartEnd()[0], counters);
        } else {
            OneDReader.recordPattern(row, pattern.getStartEnd()[1] + 1, counters);
            i = 0;
            for (int j = counters.length - 1; i < j; j--) {
                int temp = counters[i];
                counters[i] = counters[j];
                counters[j] = temp;
                i++;
            }
        }
        if (outsideChar) {
            numModules = 16;
        } else {
            numModules = 15;
        }
        float elementWidth = ((float) AbstractRSSReader.count(counters)) / ((float) numModules);
        int[] oddCounts = getOddCounts();
        int[] evenCounts = getEvenCounts();
        float[] oddRoundingErrors = getOddRoundingErrors();
        float[] evenRoundingErrors = getEvenRoundingErrors();
        for (i = 0; i < counters.length; i++) {
            float value = ((float) counters[i]) / elementWidth;
            int count = (int) (0.5f + value);
            if (count < 1) {
                count = 1;
            } else if (count > 8) {
                count = 8;
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
        adjustOddEvenCounts(outsideChar, numModules);
        int oddSum = 0;
        int oddChecksumPortion = 0;
        for (i = oddCounts.length - 1; i >= 0; i--) {
            oddChecksumPortion = (oddChecksumPortion * 9) + oddCounts[i];
            oddSum += oddCounts[i];
        }
        int evenChecksumPortion = 0;
        int evenSum = 0;
        for (i = evenCounts.length - 1; i >= 0; i--) {
            evenChecksumPortion = (evenChecksumPortion * 9) + evenCounts[i];
            evenSum += evenCounts[i];
        }
        int checksumPortion = oddChecksumPortion + (evenChecksumPortion * 3);
        int group;
        int oddWidest;
        int evenWidest;
        if (outsideChar) {
            if ((oddSum & 1) == 0 && oddSum <= 12 && oddSum >= 4) {
                group = (12 - oddSum) / 2;
                oddWidest = OUTSIDE_ODD_WIDEST[group];
                evenWidest = 9 - oddWidest;
                int vOdd = RSSUtils.getRSSvalue(oddCounts, oddWidest, false);
                return new DataCharacter(((vOdd * OUTSIDE_EVEN_TOTAL_SUBSET[group]) + RSSUtils.getRSSvalue(evenCounts, evenWidest, true)) + OUTSIDE_GSUM[group], checksumPortion);
            }
            throw NotFoundException.getNotFoundInstance();
        } else if ((evenSum & 1) == 0 && evenSum <= 10 && evenSum >= 4) {
            group = (10 - evenSum) / 2;
            oddWidest = INSIDE_ODD_WIDEST[group];
            evenWidest = 9 - oddWidest;
            return new DataCharacter(((RSSUtils.getRSSvalue(evenCounts, evenWidest, false) * INSIDE_ODD_TOTAL_SUBSET[group]) + RSSUtils.getRSSvalue(oddCounts, oddWidest, true)) + INSIDE_GSUM[group], checksumPortion);
        } else {
            throw NotFoundException.getNotFoundInstance();
        }
    }

    private int[] findFinderPattern(BitArray row, int rowOffset, boolean rightFinderPattern) throws NotFoundException {
        int[] counters = getDecodeFinderCounters();
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        int width = row.getSize();
        int i = false;
        while (rowOffset < width) {
            if (row.get(rowOffset)) {
                boolean isWhite = false;
            } else {
                i = true;
            }
            if (rightFinderPattern == i) {
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
                } else if (AbstractRSSReader.isFinderPattern(counters)) {
                    return new int[]{patternStart, x};
                } else {
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

    private FinderPattern parseFoundFinderPattern(BitArray row, int rowNumber, boolean right, int[] startEnd) throws NotFoundException {
        boolean firstIsBlack = row.get(startEnd[0]);
        int firstElementStart = startEnd[0] - 1;
        while (firstElementStart >= 0 && (row.get(firstElementStart) ^ firstIsBlack) != 0) {
            firstElementStart--;
        }
        firstElementStart++;
        int firstCounter = startEnd[0] - firstElementStart;
        int[] counters = getDecodeFinderCounters();
        System.arraycopy(counters, 0, counters, 1, counters.length - 1);
        counters[0] = firstCounter;
        int value = AbstractRSSReader.parseFinderValue(counters, FINDER_PATTERNS);
        int start = firstElementStart;
        int end = startEnd[1];
        if (right) {
            start = (row.getSize() - 1) - firstElementStart;
            end = (row.getSize() - 1) - end;
        }
        return new FinderPattern(value, new int[]{firstElementStart, startEnd[1]}, start, end, rowNumber);
    }

    private void adjustOddEvenCounts(boolean outsideChar, int numModules) throws NotFoundException {
        int i;
        boolean evenParityBad;
        int oddSum = AbstractRSSReader.count(getOddCounts());
        int evenSum = AbstractRSSReader.count(getEvenCounts());
        int mismatch = (oddSum + evenSum) - numModules;
        int i2 = oddSum & 1;
        if (outsideChar) {
            i = 1;
        } else {
            i = 0;
        }
        boolean oddParityBad = i2 == i;
        if ((evenSum & 1) != 1) {
            evenParityBad = false;
        } else {
            evenParityBad = true;
        }
        boolean z = false;
        boolean decrementOdd = false;
        boolean incrementEven = false;
        boolean decrementEven = false;
        if (outsideChar) {
            if (oddSum > 12) {
                decrementOdd = true;
            } else if (oddSum < 4) {
                z = true;
            }
            if (evenSum > 12) {
                decrementEven = true;
            } else if (evenSum < 4) {
                incrementEven = true;
            }
        } else {
            if (oddSum > 11) {
                decrementOdd = true;
            } else if (oddSum < 5) {
                z = true;
            }
            if (evenSum > 10) {
                decrementEven = true;
            } else if (evenSum < 4) {
                incrementEven = true;
            }
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
                        z = true;
                        decrementEven = true;
                    }
                } else if (evenParityBad) {
                    throw NotFoundException.getNotFoundInstance();
                }
            } else if (oddParityBad) {
                if (evenParityBad) {
                    throw NotFoundException.getNotFoundInstance();
                }
                z = true;
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
        if (z) {
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
