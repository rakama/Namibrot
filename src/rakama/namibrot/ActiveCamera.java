package rakama.namibrot;

public interface ActiveCamera
{
    public boolean isZooming();
    public int getZoomX();
    public int getZoomY();
    public int getZoomMagnitude();
    public void clearZoom();
    public boolean isDragging();
    public int getDragX();
    public int getDragY();
    public void clearDrag();
}