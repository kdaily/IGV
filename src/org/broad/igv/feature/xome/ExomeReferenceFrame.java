package org.broad.igv.feature.xome;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.FeatureUtils;
import org.broad.igv.feature.Locus;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.DragEventManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.util.MessageUtils;

import java.util.List;

/**
 * @author Jim Robinson
 * @date 5/30/12
 */
public class ExomeReferenceFrame extends ReferenceFrame {

    private static Logger log = Logger.getLogger(ExomeReferenceFrame.class);

    //List<Block> blocks;
    int firstBlockIdx;
    //int endBlockIdx;

    int exomeOrigin;
    //int exomeEnd;

    //int genomeEnd;
    private int blockGap;

    public ExomeReferenceFrame(ReferenceFrame otherFrame) {

        super(otherFrame);

    }

    @Override
    public void shiftOriginPixels(double delta) {

        if (exomeOrigin == 0 && delta < 0) return;

        double shiftBP = delta * getScale();
        exomeOrigin += shiftBP;
        if (exomeOrigin < 0) exomeOrigin = 0;

        // Find exome block that contains the new position.  We're assuming is very close to the current block.
        List<Block> blocks = XomeUtils.getBlocks(getChrName());
        Block b = blocks.get(firstBlockIdx);
        int comp = b.compareExomePosition(exomeOrigin);
        if (comp > 0) {
            while (firstBlockIdx < blocks.size() - 1) {
                firstBlockIdx++;
                b = blocks.get(firstBlockIdx);
                if (b.compareExomePosition(exomeOrigin) <= 0) break;
            }
        } else if (comp < 0) {
            while (firstBlockIdx > 0) {
                firstBlockIdx--;
                b = blocks.get(firstBlockIdx);
                if (b.compareExomePosition(exomeOrigin) >= 0) break;
            }
        }

        // Find genomePosition
        double genomePosition = b.getGenomeStart() + (exomeOrigin - b.getExomeStart());

        super.setOrigin(genomePosition, true);
    }


    @Override
    public void setOrigin(double genomePosition, boolean repaint) {

        List<Block> blocks = XomeUtils.getBlocks(chrName);
        firstBlockIdx = XomeUtils.getIndexForGenomePosition(blocks, origin);
        Block firstBlock = blocks.get(firstBlockIdx);

        exomeOrigin = firstBlock.getExomeStart() + (int) (origin - firstBlock.getGenomeStart());

        if (repaint) {
            IGV.getInstance().repaintDataAndHeaderPanels();
            IGV.getInstance().repaintStatusAndZoomSlider();
        }

    }

    @Override
    public void jumpTo(String chr, int start, int end) {

        setInterval(new Locus(chr, start, end));
        IGV.getInstance().repaintDataAndHeaderPanels();
        IGV.getInstance().repaintStatusAndZoomSlider();

    }

    /**
     * Jump to a specific locus (in genome coordinates).
     *
     * @param locus
     */
    @Override
    public void setInterval(Locus locus) {

        this.initialLocus = locus;
        this.chrName = locus.getChr();
        this.origin = locus.getStart();    // Genome locus
        int genomeEnd = locus.getEnd();

        List<Block> blocks = XomeUtils.getBlocks(chrName);
        firstBlockIdx = XomeUtils.getIndexForGenomePosition(blocks, origin);
        Block firstBlock = blocks.get(firstBlockIdx);

        exomeOrigin =  origin > firstBlock.getGenomeEnd() ? firstBlock.getExomeEnd() :
                firstBlock.getExomeStart() + (int) (origin - firstBlock.getGenomeStart());

        int exomeEnd = Math.max(exomeOrigin + 40, XomeUtils.genomeToExomePosition(blocks, genomeEnd));


        int bp = exomeEnd - exomeOrigin;
        int pw = widthInPixels <= 0 ? 1000 : widthInPixels;
        locationScale = ((double) bp) / pw;
        locationScaleValid = true;

        imputeZoom(exomeOrigin, exomeEnd);
    }

    @Override
    public void zoomTo(int newZoom, double newCenter) {

        newZoom = Math.max(0, Math.min(newZoom, maxZoom));
        double zoomFactor =   Math.pow(2, newZoom - zoom);


        int currentBPLength = (int) (locationScale * widthInPixels);
        int delta = (int) (currentBPLength / (2 * zoomFactor));

        List<Block> blocks = XomeUtils.getBlocks(chrName);
        int exomeCenter = XomeUtils.genomeToExomePosition(blocks, (int) newCenter);
        exomeOrigin =  exomeCenter - delta;

        origin = XomeUtils.exomeToGenomePosition(blocks, exomeOrigin);
        locationScale /= zoomFactor;

        IGV.getInstance().repaintDataAndHeaderPanels();
        IGV.getInstance().repaintStatusAndZoomSlider();

    }



    /**
     * Return the chromosome (genomic) position corresponding to the screen pixel position.
     *
     * @param screenPosition
     * @return
     */
    @Override
    public double getChromosomePosition(int screenPosition) {

        double exomePosition = exomeOrigin + getScale() * screenPosition;
        List<Block> blocks = XomeUtils.getBlocks(getChrName());
        return XomeUtils.exomeToGenomePosition(blocks, (int) exomePosition);
    }

    @Override
    public double getEnd() {
        List<Block> blocks = XomeUtils.getBlocks(getChrName());
        int exomeEnd = exomeOrigin + (int) (locationScale * widthInPixels);
        int genomeEnd = XomeUtils.exomeToGenomePosition(blocks, exomeEnd) ;
        return genomeEnd;
    }

    public List<Block> getBlocks() {
        return XomeUtils.getBlocks(getChrName());
    }

    public int getFirstBlockIdx() {
        return firstBlockIdx;
    }

    public boolean isExomeMode() {
        return true;
    }

    public int getExomeOrigin() {
        return exomeOrigin;
    }

    public int getBlockGap() {
        return 0; //blockGap;
    }
}
