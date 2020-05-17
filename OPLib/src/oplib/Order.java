package oplib;



import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
//import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import java.sql.DriverManager;
import java.util.GregorianCalendar;

/**
 *
 * @author aytugh
 */
public class Order {

    // <editor-fold defaultstate="collapsed" desc=" Data ">
    static private String mservername;
    static private String mdbname;
    static private String url;
    static private Connection mcn;
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" Constructors ">
    //static constructor
    static {
        mservername = "MSSQLSERVER";
        mdbname = "Orders";
    }

    public Order(String uid, String pass) {

        setConnection(uid, pass);
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" Utility Functions  ">
    public boolean IsConnected()    {
        return (mcn != null ? true : false);
    }

    public void setConnection(String uid, String pass) {
        try {
            //Per microsoft documentation no need to load the driver explicitly. Get connection does that on our behalf.
            // See http://msdn.microsoft.com/en-us/library/ms378526.aspx

            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String connectionUrl = "jdbc:sqlserver://localhost\\" + mservername
                    + ";databaseName=" + mdbname + ";user=" + uid + ";password=" + pass + ";";

            mcn = DriverManager.getConnection(connectionUrl);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println("Error code:" + ex.getErrorCode());//ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

// </editor-fold>

// <editor-fold defaultstate="collapsed" desc=" Database Functions  ">

    public String getProductDetail(String id) {
        String p = "";
        try {
            Statement s = mcn.createStatement();
            String sql = String.format("Select description,price,onhand from Product where pid = '%s'", id);
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {

                p = String.format("%s,%f,%d", rs.getString(1),rs.getFloat(2),rs.getInt(3));

            }
            s.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return p;
    }

    public List<String> getProductIds() {
        ArrayList<String> rval = new ArrayList<String>();
        try {
            Statement s = mcn.createStatement();
            String sql = "Select pid from Product";
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                rval.add(rs.getString(1));
            }
            s.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return rval;
    }

    public String getCustomer(String id) {
        String name = "";
        try {
            PreparedStatement s = mcn.prepareStatement("Select name from Customer where cid = ?");
            s.setInt(1, Integer.parseInt(id));
            ResultSet rs = s.executeQuery();
            while (rs.next()) {

                name = rs.getString(1);

            }
            s.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return name;
    }

    public List<String> getCustomerOrders(String id) {

        ArrayList<String> rval = new ArrayList<String>();
        try {

            Statement s = mcn.createStatement();
            String sql = String.format("Select DISTINCT o.oid from [Orders] o inner join OrderDetails od ON o.Oid = od.Oid WHERE o.cid = %s",id);
            ResultSet rs = s.executeQuery(sql);

            while (rs.next()) {
                int oid = rs.getInt(1);

                rval.add(String.format("%d", oid));
            }

            s.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return rval;
    }


    public List<String> getOrderDetails(String oid) {
        ArrayList<String> rval = new ArrayList<String>();
        try {

            Statement s = mcn.createStatement();
            String sql = String.format("Select c.pid,c.quantity,c.price from [Orders] b INNER JOIN OrderDetails c ON b.oid=c.oid where b.oid =%s" , oid);
            ResultSet rs = s.executeQuery(sql);
            String buf ="";
            while (rs.next()) {

                String pid = rs.getString(1);
                int quantity = rs.getInt(2);
                float price = rs.getFloat(3);
                buf = String.format("%s,%d,%f",pid, quantity,price);
                rval.add(buf);
            }

            s.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return rval;
    }

    public int Purchase(String cid, List<String> od) {
        int rval = 0;
        PreparedStatement pst = null;

        // <editor-fold defaultstate="collapsed" desc=" CREATE A NEW ORDER ID ">
        try{
            pst = mcn.prepareStatement(
                    "SELECT Max(oid) FROM Orders");
            ResultSet rs = pst.executeQuery();

            int oid =0;
            //Make a new oid
            while(rs.next())
                oid = rs.getInt(1) + 1;
            pst.close();

            //</editor-fold>

            // <editor-fold defaultstate="collapsed" desc=" CREATE A NEW ORDER ">
            GregorianCalendar now = new GregorianCalendar();
            long x = now.getTimeInMillis();
            java.sql.Date t = new java.sql.Date(x);
            String ts = t.toString();
            //Normally I would have combined this SQL query with the set of code below since they are
            // all update queries. I separated this to show you how to use PreparedStatements with
            // parameter queries.
            int nt = 2*od.size() +1;
            String [] sql = new String[nt];
            sql[0] = String.format("Insert Into Orders(oid,[Date],cid) Values (%s, '%s', %s);",oid,ts,cid);

            //</editor-fold>

            // <editor-fold defaultstate="collapsed" desc=" For each item purchased create an orderdetail record and decrement the onhand by 1 ">

            for (int i=0; i < nt-1; i+=2 ) {

                String [] vals = od.get(i/2).split(",");
                sql[i+1] = String.format("Insert Into OrderDetails(oid,pid,Quantity,Price) Values (%s, '%s', %s,%s);", oid,vals[0],vals[3],vals[2]);
                sql[i+2] = String.format("UPDATE Product SET onhand = onhand-%s WHERE pid = '%s';",vals[3],vals[0]);

            }
            rval = TransactSQL(sql);

            // </editor-fold>
        }
        catch (SQLException ex) {rval =-1; }
        return rval;
    }

    private int TransactSQL(String[] sql) {
        Statement st = null;
        int n = 0;
        try {
            st = mcn.createStatement();
            //System.out.println(mcn.getAutoCommit());
            mcn.setAutoCommit(false);

            for (int i = 0; i < sql.length; i++) {
                n += st.executeUpdate(sql[i]);
            }
            //mcn.rollback();
            mcn.commit();
        } catch (SQLException ex) {
            try {
                //rollback if there is an error
                mcn.rollback();
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return n;
    }

// </editor-fold>

    public static void main(String[] args) {
        // TODO code application logic here
        Order o = new Order("ism6236","ism6236bo");
        String name = o.getCustomer("1");
        System.out.println(name);
    }
}


