package jdbcclient;


import java.io.PrintStream;
//import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

import oplib.Order;

public class OrderProcessing {
    public static void main(String[] args) {

        // TODO code application logic here
        PrintStream cout = System.out;
        Scanner cin = new Scanner(System.in);

        cout.println("Please enter your password");
        Order db = null;
        int count = 0;
        String pass = cin.nextLine();
        db = new Order("ism6236", pass);

        List<String> aclist = db.getProductIds();
        for (String s : aclist) {
            String p = db.getProductDetail(s);
            cout.println(String.format("%s --> %s", s, p));
        }

        cout.println();
        cout.print("Enter P to purchase,  L to list customer orders , Q to quit");
        cout.flush();
        String input = cin.nextLine();
        boolean quit = false;
        ArrayList<String> ord = new ArrayList<String>();
        while (!quit) {
            int c = input.charAt(0);
            switch (c) {
                case 'p':
                case 'P':
                    cout.print("Enter Customer No: ");
                    cout.flush();
                    String cno = cin.nextLine();
                    while (true) {
                        cout.print("Enter Product No: ");
                        String pid = cin.nextLine();
                        cout.print("Enter Quantity: ");
                        cout.flush();
                        String q = cin.nextLine();
                        cout.print("Enter Price: ");
                        cout.flush();
                        String price = cin.nextLine();
                        String oline = String.format("%s,0,%s,%s",  pid, price, q); //the GUI had inserted the description so I am inserting 0 as a place holder
                        ord.add(oline);
                        cout.print("Enter N to add another product, P to finalize the purchase ");
                        cout.flush();
                        String d = cin.nextLine();
                        if (d.toLowerCase().equals("p")) {
                            break;
                        }
                    }
                    int n = db.Purchase(cno, ord);
                    //Update(accno, amt);
                    cout.println(String.format("%d records got updated",n));
                    break;

                case 'L':
                case 'l':
                    cout.println("Enter Customer No: ");
                    cout.flush();
                    cno = cin.nextLine();
                    List<String> l = (List<String>) db.getCustomerOrders(cno);
                    for (int i = 0; i < l.size(); i++) {
                        String oid = l.get(i);
                        List<String> od = db.getOrderDetails(oid);
                        cout.println(String.format("OrderId: %s", oid));
                        for (String s : od) {
                            cout.println(s);
                        }
                    }

                    break;
                default:
                    quit = true;

            }

            if (!quit) {
                cout.print("Enter P to purchase,  L to list customer orders , Q to quit");
                cout.flush();
                input = cin.nextLine();
            }

        }
    }

}


