import mpi.MPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

class NotifyProcesses extends Thread {
    private ReentrantLock masterLock = new ReentrantLock();
    private int numberOfProcesses = 4;
    private List<Tuple<Integer, Integer>> notifications = new ArrayList<>();
    private int me;

    NotifyProcesses(int me) {
        this.me = me;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("Thread " + me + " : " + " INTERRUPTED");
                masterLock.lock();
                while (notifications.size() > 0) {
                    Tuple<Integer, Integer> var = notifications.remove(0);
                    sendNotification(var.x, var.y,0);
                }
                masterLock.unlock();
                if (me == 0) {

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    sendNotification(-1, -1,0);
                }
                break;
            }

        }

    }

    public Integer getNumberToSend(){
        return notifications.size();
    }


    private void sendNotification(Integer variable, Integer value,Integer start) {
        System.out.println("Thread " + me + " : " + "Sending variable " + variable + " with new value " + value);
        for (int i = start; i <= numberOfProcesses; i++) {
            MPI.COMM_WORLD.Send(new int[]{variable, value}, 0, 2, MPI.INT, i, 1);
        }
        for (int i = start; i <= numberOfProcesses; i++) {
            boolean res[] = new boolean[1];
            MPI.COMM_WORLD.Recv(res, 0, 1, MPI.BOOLEAN, i, 2);
            if (!res[0]) {
                System.out.println("Thread " + me + " : " + "Process number " + i + " run into an exception !");
            }
        }
    }

    public void addNotification(Tuple<Integer, Integer> var) {
        System.out.println("Thread " + me + " : " + "Adding notification for variable " + var.x + " with value " + var.y);
        //sendNotification(var.x,var.y,0);
        masterLock.lock();
        //notifications.add(var);
        sendNotification(var.x,var.y,0);
        masterLock.unlock();
    }
}

public class Interactive {
    private Thread notifySystem, worker;
    private Scanner s;
    private String fileName;
    private Integer numberOfVariables;
    private Integer me;


    public Interactive(Integer numberOfVariables, String fileName, int me) {
        notifySystem = new NotifyProcesses(me);
        notifySystem.start();
        worker = new Worker(20, me);
        worker.start();
        this.fileName = fileName;
        this.numberOfVariables = numberOfVariables;
        this.me = me;
    }

    private void printOptions() {
        System.out.println("0.Exit");
        System.out.println("1.Assign a value");
        System.out.println("2.Compare value");
    }

    private void assignValue(int variablePredefined) {
        //System.out.println("\n\n0.Exit");
        //System.out.println("Please provide the variable to be changed and the value for it (like this : 1 3)");
        System.out.println("Thread " + me + " : " + "Assigning value");
        s.hasNextInt();
        Integer variable;
        if (variablePredefined != -1)
            variable = variablePredefined;
        else
            variable = s.nextInt();
        s.hasNextInt();
        Integer value = s.nextInt();
        if (variable == 0)
            return;
        try {
            Tuple<Integer, Integer> var = new Tuple<>(variable, value);
            if (var.x > numberOfVariables - 1) {
                System.out.println("Thread " + me + " : " + "It is not a valid variable !");
                return;
            }
            sendNotification(var);

        } catch (Exception ignored) {
            System.out.println("Thread " + me + " : " + "Second parameter is not an integer !");
        }
    }

    private void compareValue() {
        //System.out.println("\n\n0.Exit");
        //System.out.println("Please provide the variable to be compared and the value for it (like this : 1 3)");
        System.out.println("Thread " + me + " : " + "Compare value");
        s.hasNextInt();
        Integer variable = s.nextInt();
        Integer value = s.nextInt();
        if (variable == 0)
            return;
        try {
            Tuple<Integer, Integer> var = new Tuple<>(variable, value);
            if (var.x > numberOfVariables - 1) {
                System.out.println("Thread " + me + " : " + "It is not a valid variable !");
                return;
            }
            //System.out.println(variables[var.x] + " " + var.y);
            if (((Worker) worker).getVariable(var.x).equals(var.y)) {
                System.out.println("Thread " + me + " : " + "Values are equal ! Please assign a value");
                assignValue(var.x);
            } else {
                System.out.println("Thread " + me + " : " + "Values are not equal !");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void sendNotification(Tuple<Integer, Integer> var) {
        ((NotifyProcesses) notifySystem).addNotification(var);
    }

    public void show() {
        try {
            s = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Thread " + me + " : " + "File not found !");
            return;
        }

        while (true) {
            //printOptions();
            int option = 0;
            if (!s.hasNextInt()) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else
                option = s.nextInt();
            switch (option) {
                case 1:
                    assignValue(-1);
                    break;
                case 2:
                    compareValue();
                    break;
                case 0: {
                    notifySystem.interrupt();
                    return;
                }
                default:
                    System.out.println("Thread " + me + " : " + "Wrong option FOOL !");
            }
        }
    }
}
