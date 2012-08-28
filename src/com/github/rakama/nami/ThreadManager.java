package com.github.rakama.nami;

public class ThreadManager
{
    Namibrot nami;
    RenderContext[] context;
    RepaintLoop repaint;
    DrawLoop draw;
    int numThreads;
    
    public ThreadManager(Namibrot nami)
    {
        this.nami = nami;        
        numThreads = Runtime.getRuntime().availableProcessors() - 1;      
        numThreads = Math.max(1, numThreads);
    }
    
    public int getNumThreads()
    {
        return numThreads;
    }
    
    public void setNumThreads(int n)
    {
        numThreads = Math.max(1, n);
    }
    
    public boolean isRendering()
    {
        if(context == null)
            return false;
        
        for(RenderContext c : context)
            if(c != null && c.rendering)
                return true;
        
        return false;
    }
    
    public synchronized void startRepaintLoop()
    {
        if(repaint == null)
        {
            repaint = new RepaintLoop();
            Thread t = new Thread(repaint, "Repaint-Loop");
            t.setPriority(Thread.NORM_PRIORITY+1);
            t.start();
        }
    }
    
    public synchronized void startDrawLoop()
    {
        if(draw == null)
        {
            draw = new DrawLoop();
            Thread t = new Thread(draw, "Draw-Loop");
            t.start();
        }
    }
    
    public synchronized void stop()
    {
        if(draw != null)
            draw.interrupt();
        
        if(repaint != null)
            repaint.interrupt();
        
        if(nami != null)
            nami.interrupt();
                
        draw = null;
        repaint = null;
    }
    
    class RepaintLoop implements Runnable
    {        
        boolean interrupt;
        
        public void run()
        {
            long prevPaint = System.currentTimeMillis();
            int repaintTime = 30;
            
            while(!interrupt)
            {
                long time = System.currentTimeMillis();
                
                if(time - prevPaint > repaintTime)
                {
                    boolean update = false;
                    
                    update |= nami.getTheme().update((int)(time - prevPaint));
                    update |= nami.getGUI().update((int)(time - prevPaint));
                    
                    if(update)
                        nami.repaint();
                    
                    prevPaint = time;
                }
                
                sleep(5);
            }
        }
        
        public void interrupt()
        {
            interrupt = true;
        }
    }
    
    class DrawLoop implements Runnable
    {
        private int prevThreadNum;
        private int prevMaxSize;

        boolean interrupt;
        
        public DrawLoop()
        {
            prevThreadNum = -1;
            prevMaxSize = -1;
        }
        
        public void run()
        {
            while(!interrupt)
            {                
                if(numThreads <= 1)
                {
                    initThreads(1);
                    nami.draw(context[0]);
                }
                else
                {
                    switch(numThreads)
                    {
                    default:
                        thread16();
                        break;
                    case 15:
                    case 14:
                    case 13:
                    case 12:
                        thread12();
                        break;
                    case 11:
                    case 10:
                    case 9:
                    case 8:
                        thread8();
                        break;
                    case 7:
                    case 6:
                        thread6();
                        break;
                    case 5:
                    case 4:
                        thread4();
                        break;
                    case 3:
                        thread3();
                        break;
                    case 2:
                        thread2();
                        break;
                    }
                }
                
                while(!isModified())
                    sleep(5);
                
                while(isRendering())
                    sleep(5);

                nami.resume();                
            }
        }
        
        public void interrupt()
        {
            interrupt = true;
        }
        
        private boolean isModified()
        {
            boolean modified = false;

            synchronized(nami)
            {
                if(nami.getGUI().isZooming())
                    modified |= nami.applyZoom();
    
                if(nami.getGUI().isDragging())
                    modified |= nami.applyTranslate();

                if(nami.isResized())
                    modified |= nami.applyResize();
                
                if(nami.isThemeChanged())
                    modified |= nami.applyThemeChange();
            }

            return modified;
        }
        
        private void thread2()
        {            
            initThreads(2);
            DrawTask[] task = new DrawTask[2];
            task[0] = new DrawTask(context[0], 2, 1, 0, 0, 1, 0);
            task[1] = new DrawTask(context[1], 2, 1, 1, 0, 1, 1);
            if(nami.getAntialiasing())
                executeScaled(task);
            else
                execute(task);
        }

        private void thread3()
        {
            initThreads(3);
            DrawTask[] task = new DrawTask[3];            
            task[0] = new DrawTask(context[0], 3, 1, 0, 0, 1, 0);
            task[1] = new DrawTask(context[1], 3, 1, 1, 0, 1, 1);
            task[2] = new DrawTask(context[2], 3, 1, 2, 0, 1, 2);
            if(nami.getAntialiasing())
                executeScaled(task);
            else
                execute(task);
        }
        
        private void thread4()
        {
            initThreads(4);
            DrawTask[] task = new DrawTask[4];           
            task[0] = new DrawTask(context[0], 2, 2, 0, 0, 1, 0);
            task[1] = new DrawTask(context[1], 2, 2, 1, 0, 1, 1);
            task[2] = new DrawTask(context[2], 2, 2, 0, 1, 1, 2);
            task[3] = new DrawTask(context[3], 2, 2, 1, 1, 1, 3);
            if(nami.getAntialiasing())
                executeScaled(task);
            else
                execute(task);
        }

        private void thread6()
        {
            initThreads(6);
            DrawTask[] task = new DrawTask[6];           
            task[0] = new DrawTask(context[0], 3, 2, 0, 0, 1, 0);
            task[1] = new DrawTask(context[1], 3, 2, 1, 0, 1, 1);
            task[2] = new DrawTask(context[2], 3, 2, 2, 0, 1, 2);
            task[3] = new DrawTask(context[3], 3, 2, 0, 1, 1, 3);
            task[4] = new DrawTask(context[4], 3, 2, 1, 1, 1, 4);
            task[5] = new DrawTask(context[5], 3, 2, 2, 1, 1, 5);
            if(nami.getAntialiasing())
                executeScaled(task);
            else
                execute(task);
        }

        private void thread8()
        {
            initThreads(8);
            DrawTask[] task = new DrawTask[8];           
            task[0] = new DrawTask(context[0], 4, 2, 0, 0, 1, 0);
            task[1] = new DrawTask(context[1], 4, 2, 1, 0, 1, 1);
            task[2] = new DrawTask(context[2], 4, 2, 2, 0, 1, 2);
            task[3] = new DrawTask(context[3], 4, 2, 3, 0, 1, 3);
            task[4] = new DrawTask(context[4], 4, 2, 0, 1, 1, 4);
            task[5] = new DrawTask(context[5], 4, 2, 1, 1, 1, 5);
            task[6] = new DrawTask(context[6], 4, 2, 2, 1, 1, 6);
            task[7] = new DrawTask(context[7], 4, 2, 3, 1, 1, 7);
            if(nami.getAntialiasing())
                executeScaled(task);
            else
                execute(task);
        }

        private void thread12()
        {
            initThreads(12);
            DrawTask[] task = new DrawTask[12];            
            task[0] = new DrawTask(context[0], 3, 4, 0, 0, 1, 0);
            task[1] = new DrawTask(context[1], 3, 4, 1, 0, 1, 1);
            task[2] = new DrawTask(context[2], 3, 4, 2, 0, 1, 2);
            task[3] = new DrawTask(context[3], 3, 4, 0, 1, 1, 3);
            task[4] = new DrawTask(context[4], 3, 4, 1, 1, 1, 4);
            task[5] = new DrawTask(context[5], 3, 4, 2, 1, 1, 5);
            task[6] = new DrawTask(context[6], 3, 4, 0, 2, 1, 6);
            task[7] = new DrawTask(context[7], 3, 4, 1, 2, 1, 7);
            task[8] = new DrawTask(context[8], 3, 4, 2, 2, 1, 8);
            task[9] = new DrawTask(context[9], 3, 4, 0, 3, 1, 9);
            task[10] = new DrawTask(context[10], 3, 4, 1, 3, 1, 10);
            task[11] = new DrawTask(context[11], 3, 4, 2, 3, 1, 11);
            if(nami.getAntialiasing())
                executeScaled(task);
            else
                execute(task);
        }

        private void thread16()
        {
            initThreads(16);
            DrawTask[] task = new DrawTask[16];            
            task[0] = new DrawTask(context[0], 4, 4, 0, 0, 1, 0);
            task[1] = new DrawTask(context[1], 4, 4, 1, 0, 1, 1);
            task[2] = new DrawTask(context[2], 4, 4, 2, 0, 1, 2);
            task[3] = new DrawTask(context[3], 4, 4, 3, 0, 1, 3);
            task[4] = new DrawTask(context[4], 4, 4, 0, 1, 1, 4);
            task[5] = new DrawTask(context[5], 4, 4, 1, 1, 1, 5);
            task[6] = new DrawTask(context[6], 4, 4, 2, 1, 1, 6);
            task[7] = new DrawTask(context[7], 4, 4, 3, 1, 1, 7);
            task[8] = new DrawTask(context[8], 4, 4, 0, 2, 1, 8);
            task[9] = new DrawTask(context[9], 4, 4, 1, 2, 1, 9);
            task[10] = new DrawTask(context[10], 4, 4, 2, 2, 1, 10);
            task[11] = new DrawTask(context[11], 4, 4, 3, 2, 1, 11);
            task[12] = new DrawTask(context[12], 4, 4, 0, 3, 1, 12);
            task[13] = new DrawTask(context[13], 4, 4, 1, 3, 1, 13);
            task[14] = new DrawTask(context[14], 4, 4, 2, 3, 1, 14);
            task[15] = new DrawTask(context[15], 4, 4, 3, 3, 1, 15);
            if(nami.getAntialiasing())
                executeScaled(task);
            else
                execute(task);
        }

        private void executeScaled(DrawTask[] task)
        {
            DrawTask[] scaled = new DrawTask[task.length];
                    
            for(int i=0; i<scaled.length; i++)
                scaled[i] = task[i].getScaledCopy();
            
            execute(scaled);

            if(!nami.isInterrupted())
                execute(task);
        }
        
        private void execute(DrawTask[] task)
        {
            Thread[] thread = new Thread[task.length];
            
            for(int i=0; i<thread.length; i++)
                thread[i] = new Thread(task[i], "Draw-" + i);
            
            initThreads(thread.length);
            
            for(Thread t : thread)
                t.start();

            for(Thread t : thread)
                join(t);
        }

        private void initThreads(int n)
        {
            int maxSize = nami.getImageHeight() * nami.getImageWidth();
            
            if(prevThreadNum == n && maxSize <= prevMaxSize)
                return;
                        
            prevMaxSize = maxSize;
            prevThreadNum = n;
            context = new RenderContext[n];
            
            int bound;            
            if(n <= 1)
                bound = maxSize;
            else
                bound = (int)Math.ceil(maxSize * 1.1 / n);
                
            for(int i=0; i<n; i++)
                context[i] = new RenderContext(bound);
        }
        
        private void join(Thread thread)
        {
            try
            {
                thread.join();
            }
            catch(InterruptedException e)
            {
                  
            }   
        }
    }

    class DrawTask implements Runnable
    {
        RenderContext context;
        int scale, thread, bail;
        int skipx, skipy, offx, offy;

        public DrawTask(RenderContext context, int skipx, int skipy, int offx, int offy, 
                int scale, int thread)
        {
            this(context, skipx, skipy, offx, offy, scale, thread, 0);
        }
        
        public DrawTask(RenderContext context, int skipx, int skipy, int offx, int offy, 
                int scale, int thread, int bail)
        {
            this.context = context;
            this.skipx = skipx;
            this.skipy = skipy;
            this.offx = offx;
            this.offy = offy;
            this.scale = scale;
            this.thread = thread;
            this.bail = bail;
        }
        
        public void run()
        {
            nami.draw(context, skipx, skipy, offx, offy, scale, thread, bail);
        }
        
        public DrawTask getScaledCopy()
        {
            return new DrawTask(context, skipx, skipy, offx, offy, scale*2, thread, nami.fastIter);
        }        
    }
    
    private void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch(InterruptedException e)
        {
            
        }
    }
}