import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransportException;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.security.*;

public class ServerHandler implements ServerThrift.Iface {
  // define shared variables and locks for synchronization
  Hashmap <String,int> Fileversions;
  Address Coordinator;
  Address ServerAddress;
  String Forder;

  public ServerHandler(String dir, String s_ip, int s_port, String c_ip, int c_port)
    {
    Forder = dir;  //how to handle if there already a dir
    try{
        dir.mkdir();
    }
    catch(SecurityException se){
    }
    int c_ip=Integer.parseInt(c_ip);
    int s_ip=Integer.parseInt(s_ip);
    Coordinator=new Address(c_ip,c_port);
    ServerAddress=new Address(s_ip,s_port)
    Fileversions=new HashMap<String,int>(); //Assume no
    // reading the input file and store the node info into the list
    ServerCoordinator.Client client = null;
    try {
      TTransport transport = new TSocket(ip, port);
      TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
      ServerCoordinator client = new ServerCoordinator.Client(protocol);
      //Try to connect
      transport.open();
      if(client.join(c_ip,c_port)){// server sucessfullly join to the Coordinator
        System.out.println("Successlly Notify the Coordinator a new server join")
        System.out.println("The server has intialize");

      } else{
        System.out.println("Unable to join")
      }
      transport.close();//once the coordinator knows the server's ip address, this connection can close
    } catch(TException e) {
      e.printStackTrace();
      System.exit(0);
    }

    // nodeList.add(new NodeAddress("128.101.37.45", 9091));
    // nodeList.add(new NodeAddress("128.101.37.42", 9092));
    // nodeList.add(new NodeAddress("128.101.37.43", 9093));
  }
  @Override
    public Result Readfromfile(String filename) {
      System.out.println("sucessfullly");
}
/*  @Override
  public String submitJob(String inputDir) {
    // time point
    long starttime = System.currentTimeMillis();
    System.out.println("================ Received a new Job! ================");
    if(policy == 1) {
      System.out.println("                Scheduling Policy: Random");
    } else {
      System.out.println("             Scheduling Policy: Load-Balancing");
    }
    // get a list of input files from the input directory
    // for each of the file, start a new thread to send the request
    File folder = new File(inputDir);  // this is hardcode or pass by parameter?
    File[] listOfFiles = folder.listFiles();
    if (listOfFiles == null) {
      System.out.println("input file folder does not exist.");
      System.exit(0);
    }
    // traverse all the file entries to get the total number
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        numOfFiles++;
      }
    }
    System.out.println("                  Total Map tasks: "+numOfFiles);
    System.out.println("=====================================================");

    // Start the monitor thread to monitor the Running tasks and the Finished tasks
    Thread monitor = new Thread(new Monitor());
    monitor.start();

    // second loop to launch the thread pool
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        // start a seperate thread for each file
        Thread requestToNode = new Thread(new ComputeRequest(listOfFiles[i].getName(), policy));
        requestToNode.start();
      }
    }

    // use some synchronization mechanisms to find out if all the tasks in the fist phase are done
    synchronized(waitLock) {
      try {
        waitLock.wait();  // main thread wait here for all map tasks finish
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // then start the sort phase
    // randomly select a compute node and send the sort task to it
    NodeAddress node = nodeList.get(randomInt(nodeList.size())-1);
    String result = null;
    // Create connection to the selected compute node and send the request.
    try {
      TTransport  transport = new TSocket(node.getIpAddress(), node.getPort());
      TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
      Compute.Client client = new Compute.Client(protocol);
      transport.open();

      // Remote Procedure Call.
      result = client.handleSort("intermediate_dir");  // 1:success 2:reject 3:fail
      if (result == null) {
        System.out.println("Error: Sort phase fails");
        System.exit(0);
      }
    }
    catch(TTransportException e) {
      System.out.println("Error: Selected compute node not running. Server abort");
      System.exit(0);
    }
    catch(TException e) {
      System.out.println("Error: Server aborts due to handleSort() exception");
      System.exit(0);
    }

    // time point
    long endtime = System.currentTimeMillis();
    long timeUsed = endtime-starttime;
    System.out.println("=================== Job Finished! ===================");
    System.out.println("              "+numOfFiles+" Map tasks; 1 Sort Task");
    System.out.println("            Elapsed time: "+timeUsed+" Milliseconds    ");
    System.out.println("=====================================================");
    // 0 the numbers so that new job can use them
    numOfDone = 0;
    numOfFiles = 0;

    // return the elapsed time and the result file name
    result = result+";"+Long.toString(timeUsed);
    return result;
  }

  // helper function to get the random number within the given range
  private int randomInt(int range) {  // range is the max value that can be selected
    double randomDouble = Math.random();  // [0.0-1)
    randomDouble = randomDouble*range+1;
    return (int)randomDouble;
  }

  // nested class for the monitor thread thread
  private class Monitor implements Runnable {
    @Override
    public void run() {
      // periodically print the #running tasks and #finished tasks
      while(true) {
        synchronized(lock) {
          System.out.println("  Report: Running Map tasks:"+(numOfFiles-numOfDone)+"; Finished Map tasks:"+numOfDone);
          // test if all map tasks are finished
          if (numOfDone == numOfFiles) {
            System.out.println("  Report: Map phase done! Start sort task.");
            // notice the waiting block to get to the sort phase
            synchronized(waitLock) {
              waitLock.notify();
            }
            break;
          }
        }
        try {
          Thread.sleep(1000);  // every 1 second
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  // nested class for handling the task thread
  private class ComputeRequest implements Runnable {
    private String fileName;
    private int policy;

    // Constructor used to accept paramter from the main thread
    ComputeRequest(String fileName, int policy) {
      this.fileName = fileName;
      this.policy = policy;
    }

    @Override
    public void run() {
      // result returned by the handleMap(): 1success; 2reject; 3fail
      int result = 0;
      do {  // Use loop to handle rejection
        if(result == 2) {  // test if this is resending
          System.out.println("Re-sending the task");
        }
        if(result == 3) {
          System.out.println("Compute Node Fail");
          System.exit(0);
        }
        // randomly select a compute node
        NodeAddress node = nodeList.get(randomInt(nodeList.size())-1);
        // Create connection to the selected compute node and send the request.
        try {
          TTransport  transport = new TSocket(node.getIpAddress(), node.getPort());
          TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
          Compute.Client client = new Compute.Client(protocol);
          transport.open();

          // Remote Procedure Call.
          result = client.handleMap(fileName, policy);  // 1:success 2:reject 3:fail
        }
        catch(TTransportException e) {
          System.out.println("Selected compute node not running. Server abort");
          System.exit(0);
        }
        catch(TException e) {
          System.out.println("Error: executing map phase fail");
          System.exit(0);
        }
      // proceed when the return value is success
      } while (result != 1);

      // when response successfully, then increment the number of finished task
      synchronized(lock) {
        numOfDone++;
      }
    }

    // helper function to get the random number within the given range
    private int randomInt(int range) {  // range is the max value that can be selected
      double randomDouble = Math.random();  // [0.0-1)
      randomDouble = randomDouble*range+1;
      return (int)randomDouble;
    }
  }

  // define structure to store the node IP and port
  private class Address {
    private String ipAddress;
    private int port;
    public Address(String ipAddress, int port) {
      this.ipAddress = ipAddress;
      this.port = port;
    }
    // getters and setters
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setPort(int port) { this.port = port; }
  }

}
