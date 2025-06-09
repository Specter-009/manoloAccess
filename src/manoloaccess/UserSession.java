/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package manoloaccess;

/**
 *
 * @author Admin
 */
public class UserSession {
    private static int customerId = -1;

    public static void setCustomerId(int id) {
        customerId = id;
    }

    public static int getCustomerId() {
        return customerId;
    }

    public static void clear() {
        customerId = -1;
    }
}
