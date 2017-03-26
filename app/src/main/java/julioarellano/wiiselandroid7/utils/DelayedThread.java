package julioarellano.wiiselandroid7.utils;

public class DelayedThread extends Thread {

    private IDelayedThreadListener listener;
    private volatile boolean canFinish = true;
    private int delay;

    public interface IDelayedThreadListener {
        public void finish();

        public void stop();
    }

    public DelayedThread(int delay, IDelayedThreadListener listener) {
        this.delay = delay;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
            canFinish = false;
        }
        if (canFinish) {
            if (listener != null) {
                listener.finish();
            }
        }
    }

    public void stopDelayedThread() {
        canFinish = false;
        if (listener != null) {
            listener.stop();
        }
    }

}
