package com.google.zxing.pdf417.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.pdf417.PDF417Common;
import com.google.zxing.pdf417.decoder.ec.ErrorCorrection;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PDF417ScanningDecoder {
    private static final ErrorCorrection errorCorrection = new ErrorCorrection();

    private PDF417ScanningDecoder() {
    }

    public static DecoderResult decode(BitMatrix image, ResultPoint imageTopLeft, ResultPoint imageBottomLeft, ResultPoint imageTopRight, ResultPoint imageBottomRight, int minCodewordWidth, int maxCodewordWidth) throws NotFoundException, FormatException, ChecksumException {
        boolean leftToRight;
        BoundingBox boundingBox = new BoundingBox(image, imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight);
        DetectionResultColumn leftRowIndicatorColumn = null;
        DetectionResultColumn rightRowIndicatorColumn = null;
        DetectionResult detectionResult = null;
        int i = 0;
        while (i < 2) {
            if (imageTopLeft != null) {
                leftRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, imageTopLeft, true, minCodewordWidth, maxCodewordWidth);
            }
            if (imageTopRight != null) {
                rightRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, imageTopRight, false, minCodewordWidth, maxCodewordWidth);
            }
            detectionResult = merge(leftRowIndicatorColumn, rightRowIndicatorColumn);
            if (detectionResult != null) {
                if (i == 0 && detectionResult.getBoundingBox() != null) {
                    if (detectionResult.getBoundingBox().getMinY() < boundingBox.getMinY() || detectionResult.getBoundingBox().getMaxY() > boundingBox.getMaxY()) {
                        boundingBox = detectionResult.getBoundingBox();
                        i++;
                    }
                }
                detectionResult.setBoundingBox(boundingBox);
                break;
            }
            throw NotFoundException.getNotFoundInstance();
        }
        int maxBarcodeColumn = detectionResult.getBarcodeColumnCount() + 1;
        detectionResult.setDetectionResultColumn(0, leftRowIndicatorColumn);
        detectionResult.setDetectionResultColumn(maxBarcodeColumn, rightRowIndicatorColumn);
        if (leftRowIndicatorColumn == null) {
            leftToRight = false;
        } else {
            leftToRight = true;
        }
        for (int barcodeColumnCount = 1; barcodeColumnCount <= maxBarcodeColumn; barcodeColumnCount++) {
            int barcodeColumn;
            if (leftToRight) {
                barcodeColumn = barcodeColumnCount;
            } else {
                barcodeColumn = maxBarcodeColumn - barcodeColumnCount;
            }
            if (detectionResult.getDetectionResultColumn(barcodeColumn) == null) {
                DetectionResultColumn detectionResultColumn;
                if (barcodeColumn == 0 || barcodeColumn == maxBarcodeColumn) {
                    boolean z;
                    if (barcodeColumn != 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    detectionResultColumn = new DetectionResultRowIndicatorColumn(boundingBox, z);
                } else {
                    detectionResultColumn = new DetectionResultColumn(boundingBox);
                }
                detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn);
                int previousStartColumn = -1;
                for (int imageRow = boundingBox.getMinY(); imageRow <= boundingBox.getMaxY(); imageRow++) {
                    int startColumn = getStartColumn(detectionResult, barcodeColumn, imageRow, leftToRight);
                    if (startColumn < 0 || startColumn > boundingBox.getMaxX()) {
                        if (previousStartColumn != -1) {
                            startColumn = previousStartColumn;
                        } else {
                        }
                    }
                    Codeword codeword = detectCodeword(image, boundingBox.getMinX(), boundingBox.getMaxX(), leftToRight, startColumn, imageRow, minCodewordWidth, maxCodewordWidth);
                    if (codeword != null) {
                        detectionResultColumn.setCodeword(imageRow, codeword);
                        previousStartColumn = startColumn;
                        minCodewordWidth = Math.min(minCodewordWidth, codeword.getWidth());
                        maxCodewordWidth = Math.max(maxCodewordWidth, codeword.getWidth());
                    }
                }
            }
        }
        return createDecoderResult(detectionResult);
    }

    private static DetectionResult merge(DetectionResultRowIndicatorColumn leftRowIndicatorColumn, DetectionResultRowIndicatorColumn rightRowIndicatorColumn) throws NotFoundException, FormatException {
        if (leftRowIndicatorColumn == null && rightRowIndicatorColumn == null) {
            return null;
        }
        BarcodeMetadata barcodeMetadata = getBarcodeMetadata(leftRowIndicatorColumn, rightRowIndicatorColumn);
        return barcodeMetadata != null ? new DetectionResult(barcodeMetadata, BoundingBox.merge(adjustBoundingBox(leftRowIndicatorColumn), adjustBoundingBox(rightRowIndicatorColumn))) : null;
    }

    private static BoundingBox adjustBoundingBox(DetectionResultRowIndicatorColumn rowIndicatorColumn) throws NotFoundException, FormatException {
        if (rowIndicatorColumn == null) {
            return null;
        }
        int[] rowHeights = rowIndicatorColumn.getRowHeights();
        if (rowHeights == null) {
            return null;
        }
        int maxRowHeight = getMax(rowHeights);
        int missingStartRows = 0;
        for (int rowHeight : rowHeights) {
            missingStartRows += maxRowHeight - rowHeight;
            if (rowHeight > 0) {
                break;
            }
        }
        Codeword[] codewords = rowIndicatorColumn.getCodewords();
        int row = 0;
        while (missingStartRows > 0 && codewords[row] == null) {
            missingStartRows--;
            row++;
        }
        int missingEndRows = 0;
        for (row = rowHeights.length - 1; row >= 0; row--) {
            missingEndRows += maxRowHeight - rowHeights[row];
            if (rowHeights[row] > 0) {
                break;
            }
        }
        row = codewords.length - 1;
        while (missingEndRows > 0 && codewords[row] == null) {
            missingEndRows--;
            row--;
        }
        return rowIndicatorColumn.getBoundingBox().addMissingRows(missingStartRows, missingEndRows, rowIndicatorColumn.isLeft());
    }

    private static int getMax(int[] values) {
        int maxValue = -1;
        for (int value : values) {
            maxValue = Math.max(maxValue, value);
        }
        return maxValue;
    }

    private static BarcodeMetadata getBarcodeMetadata(DetectionResultRowIndicatorColumn leftRowIndicatorColumn, DetectionResultRowIndicatorColumn rightRowIndicatorColumn) {
        BarcodeMetadata barcodeMetadata = null;
        if (leftRowIndicatorColumn != null) {
            BarcodeMetadata leftBarcodeMetadata = leftRowIndicatorColumn.getBarcodeMetadata();
            if (leftBarcodeMetadata != null) {
                if (rightRowIndicatorColumn != null) {
                    BarcodeMetadata rightBarcodeMetadata = rightRowIndicatorColumn.getBarcodeMetadata();
                    if (rightBarcodeMetadata == null || leftBarcodeMetadata.getColumnCount() == rightBarcodeMetadata.getColumnCount() || leftBarcodeMetadata.getErrorCorrectionLevel() == rightBarcodeMetadata.getErrorCorrectionLevel() || leftBarcodeMetadata.getRowCount() == rightBarcodeMetadata.getRowCount()) {
                        return leftBarcodeMetadata;
                    }
                    return null;
                }
                return leftBarcodeMetadata;
            }
        }
        if (rightRowIndicatorColumn != null) {
            barcodeMetadata = rightRowIndicatorColumn.getBarcodeMetadata();
        }
        return barcodeMetadata;
    }

    private static DetectionResultRowIndicatorColumn getRowIndicatorColumn(BitMatrix image, BoundingBox boundingBox, ResultPoint startPoint, boolean leftToRight, int minCodewordWidth, int maxCodewordWidth) {
        DetectionResultRowIndicatorColumn rowIndicatorColumn = new DetectionResultRowIndicatorColumn(boundingBox, leftToRight);
        for (int i = 0; i < 2; i++) {
            int increment;
            if (i != 0) {
                increment = -1;
            } else {
                increment = 1;
            }
            int startColumn = (int) startPoint.getX();
            int imageRow = (int) startPoint.getY();
            while (imageRow <= boundingBox.getMaxY() && imageRow >= boundingBox.getMinY()) {
                Codeword codeword = detectCodeword(image, 0, image.getWidth(), leftToRight, startColumn, imageRow, minCodewordWidth, maxCodewordWidth);
                if (codeword != null) {
                    rowIndicatorColumn.setCodeword(imageRow, codeword);
                    if (leftToRight) {
                        startColumn = codeword.getStartX();
                    } else {
                        startColumn = codeword.getEndX();
                    }
                }
                imageRow += increment;
            }
        }
        return rowIndicatorColumn;
    }

    private static void adjustCodewordCount(DetectionResult detectionResult, BarcodeValue[][] barcodeMatrix) throws NotFoundException {
        int[] numberOfCodewords = barcodeMatrix[0][1].getValue();
        int calculatedNumberOfCodewords = (detectionResult.getBarcodeColumnCount() * detectionResult.getBarcodeRowCount()) - getNumberOfECCodeWords(detectionResult.getBarcodeECLevel());
        if (numberOfCodewords.length != 0) {
            if (numberOfCodewords[0] != calculatedNumberOfCodewords) {
                barcodeMatrix[0][1].setValue(calculatedNumberOfCodewords);
            }
        } else if (calculatedNumberOfCodewords >= 1 && calculatedNumberOfCodewords <= 928) {
            barcodeMatrix[0][1].setValue(calculatedNumberOfCodewords);
        } else {
            throw NotFoundException.getNotFoundInstance();
        }
    }

    private static DecoderResult createDecoderResult(DetectionResult detectionResult) throws FormatException, ChecksumException, NotFoundException {
        BarcodeValue[][] barcodeMatrix = createBarcodeMatrix(detectionResult);
        adjustCodewordCount(detectionResult, barcodeMatrix);
        Collection<Integer> erasures = new ArrayList();
        int[] codewords = new int[(detectionResult.getBarcodeRowCount() * detectionResult.getBarcodeColumnCount())];
        List<int[]> ambiguousIndexValuesList = new ArrayList();
        List<Integer> ambiguousIndexesList = new ArrayList();
        for (int row = 0; row < detectionResult.getBarcodeRowCount(); row++) {
            for (int column = 0; column < detectionResult.getBarcodeColumnCount(); column++) {
                int[] values = barcodeMatrix[row][column + 1].getValue();
                int codewordIndex = (detectionResult.getBarcodeColumnCount() * row) + column;
                if (values.length == 0) {
                    erasures.add(Integer.valueOf(codewordIndex));
                } else if (values.length != 1) {
                    ambiguousIndexesList.add(Integer.valueOf(codewordIndex));
                    ambiguousIndexValuesList.add(values);
                } else {
                    codewords[codewordIndex] = values[0];
                }
            }
        }
        int[][] ambiguousIndexValues = new int[ambiguousIndexValuesList.size()][];
        for (int i = 0; i < ambiguousIndexValues.length; i++) {
            ambiguousIndexValues[i] = (int[]) ambiguousIndexValuesList.get(i);
        }
        return createDecoderResultFromAmbiguousValues(detectionResult.getBarcodeECLevel(), codewords, PDF417Common.toIntArray(erasures), PDF417Common.toIntArray(ambiguousIndexesList), ambiguousIndexValues);
    }

    private static DecoderResult createDecoderResultFromAmbiguousValues(int ecLevel, int[] codewords, int[] erasureArray, int[] ambiguousIndexes, int[][] ambiguousIndexValues) throws FormatException, ChecksumException {
        int i;
        int[] ambiguousIndexCount = new int[ambiguousIndexes.length];
        int tries = 100;
        while (true) {
            int tries2 = tries - 1;
            if (tries > 0) {
                for (i = 0; i < ambiguousIndexCount.length; i++) {
                    codewords[ambiguousIndexes[i]] = ambiguousIndexValues[i][ambiguousIndexCount[i]];
                }
                try {
                    break;
                } catch (ChecksumException e) {
                    if (ambiguousIndexCount.length != 0) {
                        i = 0;
                        while (i < ambiguousIndexCount.length) {
                            if (ambiguousIndexCount[i] < ambiguousIndexValues[i].length - 1) {
                                ambiguousIndexCount[i] = ambiguousIndexCount[i] + 1;
                                tries = tries2;
                                break;
                            }
                            ambiguousIndexCount[i] = 0;
                            if (i != ambiguousIndexCount.length - 1) {
                                i++;
                            } else {
                                throw ChecksumException.getChecksumInstance();
                            }
                        }
                        tries = tries2;
                    } else {
                        throw ChecksumException.getChecksumInstance();
                    }
                }
            }
            throw ChecksumException.getChecksumInstance();
        }
        return decodeCodewords(codewords, ecLevel, erasureArray);
    }

    private static BarcodeValue[][] createBarcodeMatrix(DetectionResult detectionResult) throws FormatException {
        int column;
        int barcodeRowCount = detectionResult.getBarcodeRowCount();
        int barcodeColumnCount = detectionResult.getBarcodeColumnCount() + 2;
        BarcodeValue[][] barcodeMatrix = (BarcodeValue[][]) Array.newInstance(BarcodeValue.class, new int[]{barcodeRowCount, barcodeColumnCount});
        for (int row = 0; row < barcodeMatrix.length; row++) {
            for (column = 0; column < barcodeMatrix[row].length; column++) {
                barcodeMatrix[row][column] = new BarcodeValue();
            }
        }
        column = 0;
        for (DetectionResultColumn detectionResultColumn : detectionResult.getDetectionResultColumns()) {
            if (detectionResultColumn != null) {
                for (Codeword codeword : detectionResultColumn.getCodewords()) {
                    if (codeword != null) {
                        int rowNumber = codeword.getRowNumber();
                        if (rowNumber < 0) {
                            continue;
                        } else if (rowNumber < barcodeMatrix.length) {
                            barcodeMatrix[rowNumber][column].setValue(codeword.getValue());
                        } else {
                            throw FormatException.getFormatInstance();
                        }
                    }
                }
                continue;
            }
            column++;
        }
        return barcodeMatrix;
    }

    private static boolean isValidBarcodeColumn(DetectionResult detectionResult, int barcodeColumn) {
        return barcodeColumn >= 0 && barcodeColumn <= detectionResult.getBarcodeColumnCount() + 1;
    }

    private static int getStartColumn(DetectionResult detectionResult, int barcodeColumn, int imageRow, boolean leftToRight) {
        int offset;
        if (leftToRight) {
            offset = 1;
        } else {
            offset = -1;
        }
        Codeword codeword = null;
        if (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
            codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodeword(imageRow);
        }
        if (codeword == null) {
            codeword = detectionResult.getDetectionResultColumn(barcodeColumn).getCodewordNearby(imageRow);
            if (codeword == null) {
                if (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
                    codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodewordNearby(imageRow);
                }
                if (codeword == null) {
                    int skippedColumns = 0;
                    while (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
                        barcodeColumn -= offset;
                        Codeword[] codewords = detectionResult.getDetectionResultColumn(barcodeColumn).getCodewords();
                        int length = codewords.length;
                        int i = 0;
                        while (i < length) {
                            Codeword previousRowCodeword = codewords[i];
                            if (previousRowCodeword == null) {
                                i++;
                            } else {
                                return (!leftToRight ? previousRowCodeword.getStartX() : previousRowCodeword.getEndX()) + ((offset * skippedColumns) * (previousRowCodeword.getEndX() - previousRowCodeword.getStartX()));
                            }
                        }
                        skippedColumns++;
                    }
                    return !leftToRight ? detectionResult.getBoundingBox().getMaxX() : detectionResult.getBoundingBox().getMinX();
                }
                return !leftToRight ? codeword.getStartX() : codeword.getEndX();
            }
            return !leftToRight ? codeword.getEndX() : codeword.getStartX();
        }
        return !leftToRight ? codeword.getStartX() : codeword.getEndX();
    }

    private static Codeword detectCodeword(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight, int startColumn, int imageRow, int minCodewordWidth, int maxCodewordWidth) {
        startColumn = adjustCodewordStartColumn(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
        int[] moduleBitCount = getModuleBitCount(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
        if (moduleBitCount == null) {
            return null;
        }
        int endColumn;
        int codewordBitCount = PDF417Common.getBitCountSum(moduleBitCount);
        if (leftToRight) {
            endColumn = startColumn + codewordBitCount;
        } else {
            for (int i = 0; i < moduleBitCount.length / 2; i++) {
                int tmpCount = moduleBitCount[i];
                moduleBitCount[i] = moduleBitCount[(moduleBitCount.length - 1) - i];
                moduleBitCount[(moduleBitCount.length - 1) - i] = tmpCount;
            }
            endColumn = startColumn;
            startColumn -= codewordBitCount;
        }
        if (!checkCodewordSkew(codewordBitCount, minCodewordWidth, maxCodewordWidth)) {
            return null;
        }
        int decodedValue = PDF417CodewordDecoder.getDecodedValue(moduleBitCount);
        int codeword = PDF417Common.getCodeword(decodedValue);
        if (codeword != -1) {
            return new Codeword(startColumn, endColumn, getCodewordBucketNumber(decodedValue), codeword);
        }
        return null;
    }

    private static int[] getModuleBitCount(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight, int startColumn, int imageRow) {
        int increment;
        int imageColumn = startColumn;
        int[] moduleBitCount = new int[8];
        int moduleNumber = 0;
        if (leftToRight) {
            increment = 1;
        } else {
            increment = -1;
        }
        boolean previousPixelValue = leftToRight;
        while (true) {
            if (!leftToRight || imageColumn >= maxColumn) {
                if (!leftToRight) {
                    if (imageColumn < minColumn) {
                        break;
                    }
                }
                break;
            }
            if (moduleNumber >= moduleBitCount.length) {
                break;
            } else if (image.get(imageColumn, imageRow) != previousPixelValue) {
                moduleNumber++;
                if (previousPixelValue) {
                    previousPixelValue = false;
                } else {
                    previousPixelValue = true;
                }
            } else {
                moduleBitCount[moduleNumber] = moduleBitCount[moduleNumber] + 1;
                imageColumn += increment;
            }
        }
        if (moduleNumber != moduleBitCount.length) {
            if (!(leftToRight && imageColumn == maxColumn)) {
                if (!leftToRight) {
                    if (imageColumn != minColumn) {
                    }
                }
                return null;
            }
            if (moduleNumber != moduleBitCount.length - 1) {
                return null;
            }
        }
        return moduleBitCount;
    }

    private static int getNumberOfECCodeWords(int barcodeECLevel) {
        return 2 << barcodeECLevel;
    }

    private static int adjustCodewordStartColumn(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight, int codewordStartColumn, int imageRow) {
        int increment;
        int correctedStartColumn = codewordStartColumn;
        if (leftToRight) {
            increment = -1;
        } else {
            increment = 1;
        }
        for (int i = 0; i < 2; i++) {
            while (true) {
                if (!leftToRight || correctedStartColumn < minColumn) {
                    if (!leftToRight) {
                        if (correctedStartColumn >= maxColumn) {
                            break;
                        }
                    }
                    break;
                }
                if (leftToRight != image.get(correctedStartColumn, imageRow)) {
                    break;
                } else if (Math.abs(codewordStartColumn - correctedStartColumn) > 2) {
                    return codewordStartColumn;
                } else {
                    correctedStartColumn += increment;
                }
            }
            increment = -increment;
            if (leftToRight) {
                leftToRight = false;
            } else {
                leftToRight = true;
            }
        }
        return correctedStartColumn;
    }

    private static boolean checkCodewordSkew(int codewordSize, int minCodewordWidth, int maxCodewordWidth) {
        return minCodewordWidth + -2 <= codewordSize && codewordSize <= maxCodewordWidth + 2;
    }

    private static DecoderResult decodeCodewords(int[] codewords, int ecLevel, int[] erasures) throws FormatException, ChecksumException {
        if (codewords.length != 0) {
            int numECCodewords = 1 << (ecLevel + 1);
            int correctedErrorsCount = correctErrors(codewords, erasures, numECCodewords);
            verifyCodewordCount(codewords, numECCodewords);
            DecoderResult decoderResult = DecodedBitStreamParser.decode(codewords, String.valueOf(ecLevel));
            decoderResult.setErrorsCorrected(Integer.valueOf(correctedErrorsCount));
            decoderResult.setErasures(Integer.valueOf(erasures.length));
            return decoderResult;
        }
        throw FormatException.getFormatInstance();
    }

    private static int correctErrors(int[] codewords, int[] erasures, int numECCodewords) throws ChecksumException {
        if (erasures == null || erasures.length <= (numECCodewords / 2) + 3) {
            if (numECCodewords >= 0 && numECCodewords <= 512) {
                return errorCorrection.decode(codewords, numECCodewords, erasures);
            }
        }
        throw ChecksumException.getChecksumInstance();
    }

    private static void verifyCodewordCount(int[] codewords, int numECCodewords) throws FormatException {
        if (codewords.length >= 4) {
            int numberOfCodewords = codewords[0];
            if (numberOfCodewords > codewords.length) {
                throw FormatException.getFormatInstance();
            } else if (numberOfCodewords == 0) {
                if (numECCodewords >= codewords.length) {
                    throw FormatException.getFormatInstance();
                }
                codewords[0] = codewords.length - numECCodewords;
                return;
            } else {
                return;
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static int[] getBitCountForCodeword(int codeword) {
        int[] result = new int[8];
        int previousValue = 0;
        int i = result.length - 1;
        while (true) {
            if ((codeword & 1) != previousValue) {
                previousValue = codeword & 1;
                i--;
                if (i < 0) {
                    return result;
                }
            }
            result[i] = result[i] + 1;
            codeword >>= 1;
        }
    }

    private static int getCodewordBucketNumber(int codeword) {
        return getCodewordBucketNumber(getBitCountForCodeword(codeword));
    }

    private static int getCodewordBucketNumber(int[] moduleBitCount) {
        return ((((moduleBitCount[0] - moduleBitCount[2]) + moduleBitCount[4]) - moduleBitCount[6]) + 9) % 9;
    }
}
