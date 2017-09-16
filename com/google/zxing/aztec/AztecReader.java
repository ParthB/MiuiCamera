package com.google.zxing.aztec;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.aztec.decoder.Decoder;
import com.google.zxing.aztec.detector.Detector;
import com.google.zxing.common.DecoderResult;
import java.util.List;
import java.util.Map;

public final class AztecReader implements Reader {
    public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException {
        Exception notFoundException = null;
        FormatException formatException = null;
        Detector detector = new Detector(image.getBlackMatrix());
        ResultPoint[] resultPointArr = null;
        DecoderResult decoderResult = null;
        try {
            AztecDetectorResult detectorResult = detector.detect(false);
            resultPointArr = detectorResult.getPoints();
            decoderResult = new Decoder().decode(detectorResult);
        } catch (Exception e) {
            notFoundException = e;
        } catch (FormatException e2) {
            formatException = e2;
        }
        if (decoderResult == null) {
            try {
                detectorResult = detector.detect(true);
                resultPointArr = detectorResult.getPoints();
                decoderResult = new Decoder().decode(detectorResult);
            } catch (Exception e3) {
                if (notFoundException != null) {
                    throw notFoundException;
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            } catch (Exception e12) {
                e12.printStackTrace();
            }
        }
        if (hints != null) {
            ResultPointCallback rpcb = (ResultPointCallback) hints.get(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
            if (rpcb != null) {
                for (ResultPoint point : resultPointArr) {
                    rpcb.foundPossibleResultPoint(point);
                }
            }
        }
        Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), resultPointArr, BarcodeFormat.AZTEC);
        List<byte[]> byteSegments = decoderResult.getByteSegments();
        if (byteSegments != null) {
            result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
        }
        String ecLevel = decoderResult.getECLevel();
        if (ecLevel != null) {
            result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
        }
        return result;
        if (formatException == null) {
            throw e3;
        } else {
            throw formatException;
        }
    }

    public void reset() {
    }
}
