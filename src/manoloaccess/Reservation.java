/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package manoloaccess;

import java.awt.Image;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;

/**
 *
 * @author Admin
 */

public class Reservation extends javax.swing.JFrame {
    
    Connection con;
    private int eventId;
    private int customerId;

    public Reservation() {
        initComponents(); // Initialize the UI
        String url = "jdbc:mysql://localhost:3306/db_ticketing";
        String user = "root";
        String pass = "";
        try {
            con = DriverManager.getConnection(url, user, pass);
            System.out.println("Database connection successful at " + new java.util.Date());
            // HIGHLIGHT: Initialize customerId from UserSession
            this.customerId = UserSession.getCustomerId();
            if (this.customerId <= 0) {
                JOptionPane.showMessageDialog(null, "No customer logged in. Please log in first.");
                dispose();
            } else {
                System.out.println("Reservation initialized with customer_id: " + this.customerId);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Database connection failed: " + ex.getMessage());
        }
    }

   private void loadTicketTypes(int eventId) {
        if (con == null) {
            JOptionPane.showMessageDialog(null, "Database connection is not established.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT ticket_type FROM tickets WHERE event_id = ? ORDER BY ticket_type")) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                ticketType.removeAllItems();
                boolean hasTicketTypes = false;
                Set<String> ticketTypes = new HashSet<>();

                while (rs.next()) {
                    String ticket_Type = rs.getString("ticket_type");
                    if (ticket_Type != null && !ticketTypes.contains(ticket_Type)) {
                        ticketType.addItem(ticket_Type);
                        ticketTypes.add(ticket_Type);
                        hasTicketTypes = true;
                    }
                }

                if (!hasTicketTypes) {
                    JOptionPane.showMessageDialog(null, "No ticket types found for event ID: " + eventId);
                    ticketType.setEnabled(false);
                    seatSection.setEnabled(false);
                } else {
                    ticketType.setEnabled(true);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Failed to load ticket types: " + e.getMessage());
        }
    }

    private void loadSeatSections() {
        String selectedTicketType = (String) ticketType.getSelectedItem();
        if (selectedTicketType == null || eventId == 0) {
            seatSection.removeAllItems();
            seatSection.setEnabled(false);
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT seat_section FROM tickets WHERE event_id = ? AND ticket_type = ? ORDER BY seat_section")) {
            ps.setInt(1, eventId);
            ps.setString(2, selectedTicketType);
            try (ResultSet rs = ps.executeQuery()) {
                seatSection.removeAllItems();
                boolean hasSeatSections = false;
                Set<String> seatSections = new HashSet<>();

                while (rs.next()) {
                    String seat_Section = rs.getString("seat_section");
                    if (seat_Section != null && !seatSections.contains(seat_Section)) {
                        seatSection.addItem(seat_Section);
                        seatSections.add(seat_Section);
                        hasSeatSections = true;
                    }
                }

                if (!hasSeatSections) {
                    seatSection.setEnabled(false);
                } else {
                    seatSection.setEnabled(true);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Failed to load seat sections: " + e.getMessage());
        }
    }

    private void updatePriceAndTicketsLeft() {
        String selectedTicketType = (String) ticketType.getSelectedItem();
        String selectedSeatSection = (String) seatSection.getSelectedItem();
        if (selectedTicketType == null || selectedSeatSection == null || eventId == 0) {
            ticketPrice.setText("");
            Tickets_left.setText("");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT price, tickets_left FROM tickets WHERE event_id = ? AND ticket_type = ? AND seat_section = ?")) {
            ps.setInt(1, eventId);
            ps.setString(2, selectedTicketType);
            ps.setString(3, selectedSeatSection);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double price = rs.getDouble("price");
                    int ticketsLeft = rs.getInt("tickets_left");
                    ticketPrice.setText(String.valueOf(price));
                    Tickets_left.setText(String.valueOf(ticketsLeft));
                    updateTotalPrice(); // Update total price when price changes
                } else {
                    ticketPrice.setText("");
                    Tickets_left.setText("");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error updating price: " + e.getMessage());
        }
    }

    private void updateTotalPrice() {
        String priceText = ticketPrice.getText().trim();
        if (priceText.isEmpty()) return;

        try {
            double price = Double.parseDouble(priceText);
            int quantity = (int) qtySpinner.getValue();
            double total = price * quantity;
            totalPrice.setText(String.format("$%.2f", total));

            int ticketsLeft = Integer.parseInt(Tickets_left.getText().trim());
            if (quantity > ticketsLeft) {
                JOptionPane.showMessageDialog(null, "Not enough tickets available. Max: " + ticketsLeft);
                ((SpinnerNumberModel) qtySpinner.getModel()).setValue(ticketsLeft);
                updateTotalPrice();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid price format: " + e.getMessage());
        }
    }

    private void updatePaymentMethod() {
        String paymentMethod = (String) paymentMethodBox.getSelectedItem();
        if (!"Select".equals(paymentMethod)) {
            System.out.println("Selected payment method: " + paymentMethod + " at " + new java.util.Date());
        }
    }

private void submitOrder() {
    if (con == null) {
        JOptionPane.showMessageDialog(null, "Database connection is not established.");
        return;
    }

    String selectedTicketType = (String) ticketType.getSelectedItem();
    String selectedSeatSection = (String) seatSection.getSelectedItem();
    int quantity = (int) qtySpinner.getValue();
    double total = Double.parseDouble(totalPrice.getText().replace("$", "").trim());
    String paymentMethod = (String) paymentMethodBox.getSelectedItem();

    if (selectedTicketType == null || selectedSeatSection == null || quantity <= 0 || "Select".equals(paymentMethod)) {
        JOptionPane.showMessageDialog(null, "Please select ticket type, seat section, quantity, and payment method.");
        return;
    }

    try {
        System.out.println("Attempting to fetch ticket_id for event_id=" + eventId + ", ticket_type=" + selectedTicketType + ", seat_section=" + selectedSeatSection + " at " + new java.util.Date());
        int ticketId = -1;
        String ticketSql = "SELECT ticket_id, tickets_left FROM tickets WHERE event_id = ? AND ticket_type = ? AND seat_section = ?";
        try (PreparedStatement ticketPs = con.prepareStatement(ticketSql)) {
            ticketPs.setInt(1, eventId);
            ticketPs.setString(2, selectedTicketType);
            ticketPs.setString(3, selectedSeatSection);
            try (ResultSet rs = ticketPs.executeQuery()) {
                if (rs.next()) {
                    ticketId = rs.getInt("ticket_id");
                    System.out.println("Fetched ticket_id: " + ticketId + " with tickets_left: " + rs.getInt("tickets_left"));
                    int ticketsLeft = rs.getInt("tickets_left");
                    if (quantity > ticketsLeft) {
                        JOptionPane.showMessageDialog(null, "Not enough tickets available. Max: " + ticketsLeft);
                        return;
                    }
                } else {
                    System.out.println("No ticket found for the given criteria.");
                    JOptionPane.showMessageDialog(null, "No ticket found for selected options.");
                    return;
                }
            }
        }

        if (ticketId == -1) {
            System.out.println("Invalid ticket_id detected.");
            JOptionPane.showMessageDialog(null, "Invalid ticket ID.");
            return;
        }

        System.out.println("Using customer_id: " + customerId + " for order at " + new java.util.Date());
        if (customerId <= 0) {
            JOptionPane.showMessageDialog(null, "Invalid customer ID. Please log in or select a customer.");
            return;
        }

        // Generate a unique payment reference
        String paymentReference = generatePaymentReference();

        // Insert into orders with auto-commit
        int orderId = -1;
        con.setAutoCommit(true);
        try (PreparedStatement orderPs = con.prepareStatement("INSERT INTO orders (customer_id, total_amount, quantity, payment_method, payment_reference) VALUES (?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            orderPs.setInt(1, customerId);
            orderPs.setDouble(2, total);
            orderPs.setInt(3, quantity);
            orderPs.setString(4, paymentMethod);
            orderPs.setString(5, paymentReference);
            int rowsAffected = orderPs.executeUpdate();
            System.out.println("Inserted into orders, rows affected: " + rowsAffected);
            try (ResultSet generatedKeys = orderPs.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);
                    System.out.println("Generated order_id: " + orderId + " with payment_reference: " + paymentReference);
                }
            }
        }

        if (orderId == -1) {
            JOptionPane.showMessageDialog(null, "Failed to generate order ID.");
            return;
        }

        // Insert into order_tickets with the committed order_id
        con.setAutoCommit(false);
        try {
            // Update tickets table with quantity subtraction
            System.out.println("Executing UPDATE with ticket_id=" + ticketId + ", quantity=" + quantity + " at " + new java.util.Date());
            try (PreparedStatement updatePs = con.prepareStatement("UPDATE tickets SET tickets_left = tickets_left - ? WHERE ticket_id = ? AND tickets_left >= ?")) {
                updatePs.setInt(1, quantity);
                updatePs.setInt(2, ticketId);
                updatePs.setInt(3, quantity);
                int updatedRows = updatePs.executeUpdate();
                System.out.println("Updated rows in tickets: " + updatedRows);
                if (updatedRows == 0) {
                    JOptionPane.showMessageDialog(null, "Order failed: Not enough tickets or invalid ticket ID.");
                    con.rollback();
                    return;
                }
            }

            // Insert into order_tickets
            System.out.println("Inserting into order_tickets with order_id=" + orderId + ", ticket_id=" + ticketId + " at " + new java.util.Date());
            try (PreparedStatement junctionPs = con.prepareStatement("INSERT INTO order_tickets (order_tickets_id, order_id, ticket_id) VALUES (NULL, ?, ?)")) {
                junctionPs.setInt(1, orderId);
                junctionPs.setInt(2, ticketId);
                int junctionRows = junctionPs.executeUpdate();
                System.out.println("Inserted into order_tickets, rows affected: " + junctionRows);
            }

            con.commit();
            System.out.println("Transaction committed for order_id: " + orderId);
            JOptionPane.showMessageDialog(null, "Order submitted successfully! Total: " + totalPrice.getText() + ", Reference: " + paymentReference);
            updatePriceAndTicketsLeft();
            qtySpinner.setValue(1);
            paymentMethodBox.setSelectedIndex(0);
        } catch (SQLException e) {
            con.rollback();
            System.out.println("SQLException during transaction: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Error submitting order: " + e.getMessage());
        } finally {
            con.setAutoCommit(true);
        }
    } catch (SQLException | NumberFormatException e) {
        System.out.println("General exception: " + e.getMessage());
        JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage());
    }
}
    // Helper method to generate a unique payment reference
    private String generatePaymentReference() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomStr = Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 8).toUpperCase();
        return "REF-" + timestamp + "-" + randomStr;
    }



    public void setEventData(int eventId, byte[] imageData, String eventName, String eventDate, String description,
                             String category, String venueName, String ticket_Type, String seatSectionVal,
                             double price, int ticketsLeft) {
        this.eventId = eventId;
        System.out.println("setEventData called with eventId: " + eventId);

        if (imageData != null) {
            ImageIcon icon = new ImageIcon(imageData);
            Image scaled = icon.getImage().getScaledInstance(EventImageContainer.getWidth(), EventImageContainer.getHeight(), Image.SCALE_SMOOTH);
            EventImageContainer.setIcon(new ImageIcon(scaled));
        }

        EventName.setText(eventName);
        Date.setText(eventDate);
        Description.setText(description);
        Category.setText(category);
        VenueName.setText(venueName);
        
        loadTicketTypes(eventId);

        if (ticket_Type != null && !ticket_Type.isEmpty()) {
            ticketType.setSelectedItem(ticket_Type);
            loadSeatSections();
            if (seatSectionVal != null && !seatSectionVal.isEmpty()) {
                seatSection.setSelectedItem(seatSectionVal);
            }
        }

        ticketPrice.setText(String.valueOf(price));
        Tickets_left.setText(String.valueOf(ticketsLeft));
        updateTotalPrice();
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
        System.out.println("Customer ID set to: " + customerId + " at " + new java.util.Date());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        backButton = new javax.swing.JButton();
        EventName = new javax.swing.JTextField();
        VenueName = new javax.swing.JTextField();
        Date = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        seatSection = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        ticketType = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        ticketPrice = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        qtySpinner = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        totalPrice = new javax.swing.JTextField();
        CheckOut = new javax.swing.JButton();
        CancelButton = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        paymentMethodBox = new javax.swing.JComboBox();
        Tickets_left = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        EventImageContainer = new javax.swing.JLabel();
        Description = new javax.swing.JTextField();
        Category = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBackground(new java.awt.Color(0, 204, 255));

        backButton.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        backButton.setText("<");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(backButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(66, Short.MAX_VALUE)
                .addComponent(backButton)
                .addContainerGap())
        );

        EventName.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N

        VenueName.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N

        Date.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N

        jPanel3.setBackground(new java.awt.Color(255, 155, 50));

        jLabel2.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel2.setText("Seat Section");

        seatSection.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        seatSection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seatSectionActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel3.setText("Ticket Type");

        ticketType.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        ticketType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ticketTypeActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel4.setText("Ticket Price");

        ticketPrice.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel5.setText("Qty");

        qtySpinner.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        qtySpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                qtySpinnerStateChanged(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel6.setText("You can't order different seat type or ticket type at the same time");

        jLabel7.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel7.setText("Total Price");

        totalPrice.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N

        CheckOut.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        CheckOut.setText("Check Out");
        CheckOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckOutActionPerformed(evt);
            }
        });

        CancelButton.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        CancelButton.setText("Cancel");
        CancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelButtonActionPerformed(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel8.setText("Payment Method");

        paymentMethodBox.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        paymentMethodBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GCash", "Maya", "ShopeePay", "Coinsph", "BDO", "BPI", "UnionBank", "Metrobank", "Visa", "Mastercard", "7Eleven", "Palawan Express", "Cebuana Lhuillier", "MLhuillier", "PayPal", "Dragonpay", "GrabPay" }));
        paymentMethodBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paymentMethodBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(116, 116, 116)
                        .addComponent(jLabel6))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(totalPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(CheckOut)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CancelButton))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4))
                                .addGap(43, 43, 43)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(ticketType, javax.swing.GroupLayout.PREFERRED_SIZE, 379, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(seatSection, javax.swing.GroupLayout.PREFERRED_SIZE, 379, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(ticketPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(qtySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(paymentMethodBox, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap(157, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(ticketType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(seatSection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(ticketPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(qtySpinner))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(paymentMethodBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(275, 275, 275)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(totalPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CheckOut)
                    .addComponent(CancelButton))
                .addGap(83, 83, 83))
        );

        Tickets_left.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        Tickets_left.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Tickets_leftActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel1.setText("Tickets Left");

        EventImageContainer.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        Description.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N

        Category.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(EventImageContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap(49, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Description)
                            .addComponent(VenueName, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(Date, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(18, 18, 18)
                                .addComponent(Tickets_left, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(EventName)
                            .addComponent(Category))))
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(EventImageContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addComponent(EventName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(VenueName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Description)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Category, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Date, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(Tickets_left, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(65, 65, 65))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Tickets_leftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Tickets_leftActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_Tickets_leftActionPerformed

    private void ticketTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ticketTypeActionPerformed
        // TODO add your handling code here:
        updatePriceAndTicketsLeft();
        loadSeatSections();
    }//GEN-LAST:event_ticketTypeActionPerformed

    private void seatSectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_seatSectionActionPerformed
        // TODO add your handling code here:
        updatePriceAndTicketsLeft();
    }//GEN-LAST:event_seatSectionActionPerformed

    private void qtySpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_qtySpinnerStateChanged
        // TODO add your handling code here:
        updateTotalPrice();
    }//GEN-LAST:event_qtySpinnerStateChanged

    private void paymentMethodBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paymentMethodBoxActionPerformed
        // TODO add your handling code here:
        updatePaymentMethod();
    }//GEN-LAST:event_paymentMethodBoxActionPerformed

    private void CheckOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckOutActionPerformed
        // TODO add your handling code here:
        submitOrder();
        UserInterface userint = new UserInterface();
        userint.setVisible(true);
        dispose();
    }//GEN-LAST:event_CheckOutActionPerformed

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        // TODO add your handling code here:
        UserInterface userint = new UserInterface();
        userint.setVisible(true);
        dispose();
    }//GEN-LAST:event_backButtonActionPerformed

    private void CancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CancelButtonActionPerformed
        // TODO add your handling code here:
        UserInterface userint = new UserInterface();
        userint.setVisible(true);
        dispose();
    }//GEN-LAST:event_CancelButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Reservation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Reservation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Reservation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Reservation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Reservation().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton CancelButton;
    private javax.swing.JTextField Category;
    private javax.swing.JButton CheckOut;
    private javax.swing.JTextField Date;
    private javax.swing.JTextField Description;
    private javax.swing.JLabel EventImageContainer;
    private javax.swing.JTextField EventName;
    private javax.swing.JTextField Tickets_left;
    private javax.swing.JTextField VenueName;
    private javax.swing.JButton backButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JComboBox paymentMethodBox;
    private javax.swing.JSpinner qtySpinner;
    private javax.swing.JComboBox seatSection;
    private javax.swing.JTextField ticketPrice;
    private javax.swing.JComboBox ticketType;
    private javax.swing.JTextField totalPrice;
    // End of variables declaration//GEN-END:variables
}
