import mpi.MPI;
import mpi.Status;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;


public class Worker extends Thread {
    private Integer[] variables;
    private Integer procNumber;
    private ReentrantLock masterLock = new ReentrantLock();

    Worker(Integer numberOfVariables, Integer procNumber) {

        variables = new Integer[numberOfVariables];
        this.procNumber = procNumber;
        for (int i=0;i<variables.length;i++)
           variables[i]=0;
    }

    private Boolean changeVariable(Integer variable,Integer value){
        try{
            variables[variable] = value;
            System.out.println("Process " + procNumber + " : variable " + variable + " changed to " + value);
            return true;
        }catch (Exception e){
            System.out.println("Process " + procNumber + " : an error occured when changing variable");
            return true;
        }
    }

    @Override
    public void run() {
        while (true){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }

            int[] rec = new int[2];
            Status status = MPI.COMM_WORLD.Recv(rec, 0, 2, MPI.INT, MPI.ANY_SOURCE, 1);
            masterLock.lock();
            if (rec[0]==-1) {
                MPI.COMM_WORLD.Ssend(new boolean[]{true}, 0, 1, MPI.BOOLEAN, status.source, 2);
                break;
            }
            Boolean response = changeVariable(rec[0],rec[1]);
            MPI.COMM_WORLD.Ssend(new boolean[]{response}, 0, 1, MPI.BOOLEAN, status.source, 2);
            masterLock.unlock();
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("file" + procNumber + ".txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i=0;i<variables.length;i++) {
            try {
                writer.write(variables[i] + " ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Integer getVariable(Integer variable) throws ArrayIndexOutOfBoundsException{
        masterLock.lock();
        Integer ret = variables[variable];
        masterLock.unlock();
        return ret;
    }

    public void setVariable(Integer variable, Integer value) throws ArrayIndexOutOfBoundsException{
        masterLock.lock();
        variables[variable] = value;
        masterLock.unlock();
    }
}
