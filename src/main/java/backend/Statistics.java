package backend;

import balok.causality.Epoch;
import balok.ser.SerializedFrame;
import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntLongHashMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import com.carrotsearch.hppc.cursors.IntLongCursor;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Statistics {

    private String benchmarkName;

    private IntLongHashMap accessDistribution = new IntLongHashMap();

    public Statistics(String benchmarkName) {
        this.benchmarkName = benchmarkName;
    }

    public void addFrame(SerializedFrame<Epoch> frame) {
        for (int i = 0; i < frame.size(); i++) {
            long numOfAccess = accessDistribution.getOrDefault(frame.getAddresses()[i], 0l) + 1;
            accessDistribution.put(frame.getAddresses()[i], numOfAccess);
        }
    }

    public void generateFigure() {
        System.out.println("Generating figure, total number of memory locations is " + accessDistribution.size());
        // bar chart
//        CategoryChart accessDistributionChart = new CategoryChartBuilder().width(800).height(600)
//                .title("Memory Access Distribution").xAxisTitle("Address").yAxisTitle("Number of Accesses").build();
//        Styler styler = accessDistributionChart.getStyler();
//        //styler.setLegendPosition(Styler.LegendPosition.InsideNE);
//        styler.setHasAnnotations(true);
//        int[] keys = new int[accessDistribution.size()];
//        int[] values = new int[accessDistribution.size()];
//        int index = 0;
//        Iterator<IntIntCursor> iter = accessDistribution.iterator();
//        while (iter.hasNext()) {
//            IntIntCursor cursor = iter.next();
//            keys[index] = cursor.key;
//            values[index] = cursor.value;
//            index++;
//        }
//        accessDistributionChart.addSeries("access", keys, values);

        Random random = new Random();
        // line chart
        final int nodeNum = 100;
        XYChart accessDistributionChart = new XYChartBuilder().width(800).height(600)
                .title("Memory Access Distribution").xAxisTitle("Address").yAxisTitle("Number of Accesses").build();
        accessDistributionChart.getStyler().setChartTitleVisible(true);
        accessDistributionChart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        accessDistributionChart.getStyler().setMarkerSize(1);
        accessDistributionChart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        ArrayList<Integer> keyList = new ArrayList<>(accessDistribution.size());
        ArrayList<Long> valueList = new ArrayList<>(accessDistribution.size());
        Iterator<IntLongCursor> iter = accessDistribution.iterator();
        while (iter.hasNext()) {
            IntLongCursor cursor = iter.next();
            keyList.add(cursor.key);
            valueList.add(cursor.value);
        }

        quickSort(keyList, valueList, 0, keyList.size(), random, Comparators.intComparator);
        ArrayList<Integer> sampleKeys = new ArrayList<>(nodeNum);
        ArrayList<Long> sampleValues = new ArrayList<>(nodeNum);
        int stride = keyList.size() / nodeNum;
        int remain = keyList.size() % nodeNum;
        int index2 = -1;
        for (int i = 0; i < nodeNum; i++) {
            index2 = (i < remain) ? index2 + stride + 1 : index2 + stride;
            sampleKeys.add(keyList.get(index2));
            sampleValues.add(valueList.get(index2));
        }
        accessDistributionChart.addSeries("sampled access", sampleKeys, sampleValues);

        //quickSort(valueList, keyList, 0, valueList.size(), random, Comparators.longComparator);
        final int THRESHOLD = 10000;
        valueList.sort(Comparators.longComparator);
        ArrayList<Long> accessNumList = new ArrayList<>();
        ArrayList<Long> addressNumList = new ArrayList<>();
        long previousAccessNum = valueList.get(0);
        long previousAddressNum = 0;
        for (long accessNum : valueList) {
            if (accessNum == previousAccessNum) {
                previousAddressNum++;
            } else {
                if (previousAccessNum > THRESHOLD) {
                    accessNumList.add(previousAccessNum);
                    addressNumList.add(previousAddressNum);
                    System.out.println("accessNum: " + previousAccessNum + ", addressNum: " + previousAddressNum);
                }

                if (previousAccessNum <= 10) {
                    System.out.println("accessNum: " + previousAccessNum + ", addressNum: " + previousAddressNum);
                }
                previousAccessNum = accessNum;
                previousAddressNum = 1;
            }
        }
        if (previousAccessNum > THRESHOLD) {
            accessNumList.add(previousAccessNum);
            addressNumList.add(previousAddressNum);
            System.out.println("accessNum: " + previousAccessNum + ", addressNum: " + previousAddressNum);
        }
        XYChart accessNumDistributionChart = new XYChartBuilder().width(800).height(600)
                .title("Memory Access Number Distribution").xAxisTitle("Number of Accesses").yAxisTitle("Number of Addresses").build();
        accessNumDistributionChart.getStyler().setChartTitleVisible(true);
        accessNumDistributionChart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        accessNumDistributionChart.getStyler().setMarkerSize(6);
        //accessNumDistributionChart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        accessNumDistributionChart.getStyler().setYAxisLogarithmic(true);
        XYSeries series1 = accessNumDistributionChart.addSeries("access", accessNumList, addressNumList);
        series1.setMarker(SeriesMarkers.CIRCLE).setMarkerColor(Color.YELLOW).setLineStyle(SeriesLines.SOLID);

        System.out.println("Exporting figure as PNG......");
        try {
            BitmapEncoder.saveBitmap(accessDistributionChart, benchmarkName + "-MemoryAccessDistribution", BitmapEncoder.BitmapFormat.PNG);
            BitmapEncoder.saveBitmap(accessNumDistributionChart, benchmarkName + "-MemoryAccessNumberDistribution", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }


//        System.out.println("Exporting figure as PDF......");
//        try {
//            VectorGraphicsEncoder.saveVectorGraphic(accessDistributionChart, benchmarkName + "-MemoryAccessDistribution", VectorGraphicsEncoder.VectorGraphicsFormat.PDF);
//            VectorGraphicsEncoder.saveVectorGraphic(accessNumDistributionChart, benchmarkName + "-MemoryAccessNumberDistribution", VectorGraphicsEncoder.VectorGraphicsFormat.PDF);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    <T1 extends Number, T2 extends Number> void quickSort(List<T1> keys, List<T2> values, final int start, final int end, Random rr, Comparator<T1> comp) {
        if (end - start <= 1) {
            return;
        }
        int pivalIndex = rr.nextInt(end - start) + start;
        T1 tmp = keys.get(start);
        keys.set(start, keys.get(pivalIndex));
        keys.set(pivalIndex, tmp);
        T2 tmp2 = values.get(start);
        values.set(start, values.get(pivalIndex));
        values.set(pivalIndex, tmp2);

        T1 pival = keys.get(start);

        int begin = start + 1;
        int finish = end - 1;
        while (begin <= finish) {
            for (; begin <= finish; begin++) {
                if (comp.compare(keys.get(begin), pival) > 0) {
                    break;
                }
            }
            for (; begin <= finish; finish--) {
                if (comp.compare(keys.get(finish), pival) < 0) {
                    break;
                }
            }
            if (begin <= finish) {
                tmp = keys.get(begin);
                keys.set(begin, keys.get(finish));
                keys.set(finish, tmp);
                tmp2 = values.get(begin);
                values.set(begin, values.get(finish));
                values.set(finish, tmp2);
            }
        }
        tmp = keys.get(begin - 1);
        keys.set(begin - 1, keys.get(start));
        keys.set(start, tmp);
        tmp2 = values.get(begin - 1);
        values.set(begin - 1, values.get(start));
        values.set(start, tmp2);
        quickSort(keys, values, start, begin - 1, rr, comp);
        quickSort(keys, values, begin, end, rr, comp);
    }
}
