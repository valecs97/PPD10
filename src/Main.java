import mpi.MPI;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        if (me == 0) {
            Interactive view = new Interactive(20,"command1.txt",me);
            view.show();
            System.out.println("Main 1 terminated");
        }else if (me == 1)
        {
            Interactive view = new Interactive(20,"command2.txt",me);
            view.show();
            System.out.println("Main 2 terminated");
        }
        else {
            Thread worker = new Worker(20,me);
            worker.start();
            worker.join();
            System.out.println("Process " + me + " : terminated");
        }

        MPI.Finalize();
    }
}
