package net.engineerAnsh.BankApplication.Services;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Dto.Statements.StatementRowDto;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

@Service
public class StatementPdfService {

    // It returns the PDF as a byte[], so it can be: downloaded, emailed, stored...
    public byte[] generatePdf(AccountStatementDto statement) {

        // Output Stream (Where PDF is written)...
        // Why this is needed: PDF is created in memory, Not saved to disk, It is :- Safe, fast, cloud-friendly
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Represents the PDF file itself, Everything you add goes inside this document...
        Document document = new Document();

        try {
            // Attach Writer to Document...
            PdfWriter.getInstance(document, out);

            // Open the Document
            document.open(); // Starts writing to the PDF, You must open before adding content...

            // Header Section
            document.add(new Paragraph("BANK OF ANSH"));
            document.add(new Paragraph("Account Statement\n\n"));

            // Account Information Section
            document.add(new Paragraph("Account Holder: " + statement.getAccountHolderName()));
            document.add(new Paragraph("Account Number: " + statement.getMaskedAccountNumber()));
            document.add(new Paragraph("Account Type: " + statement.getAccountType()));
            document.add(new Paragraph("Period: " + statement.getFromDate() + " to " + statement.getToDate()));
            document.add(new Paragraph("\n"));

            // Create Table with 5 Columns : Date, Description, Debit, Credit, Balance...
            PdfPTable table = new PdfPTable(5);

            // Add Table Headers...
            // This is the statement header row.
            table.addCell("Date");
            table.addCell("Description");
            table.addCell("Debit");
            table.addCell("Credit");
            table.addCell("Balance");

            // Add Transaction Rows:-
            for (StatementRowDto row : statement.getTransactions()) {
                table.addCell(row.getDate().toString());
                table.addCell(row.getDescription());
                // If debit exists → show amount, Else → show '-'
                // If credit exists → show amount, Else → show '-'
                table.addCell(row.getDebit() != null ? row.getDebit().toString() : "-");
                table.addCell(row.getCredit() != null ? row.getCredit().toString() : "-");
                table.addCell(row.getBalance().toString()); // Balance after the transaction...
            }

            // Now the table becomes part of the PDF.
            document.add(table);

            // Footer Section...
            document.add(new Paragraph("\nClosing Balance: ₹" + statement.getClosingBalance()));
            document.add(new Paragraph("\nThis is a system generated statement."));

            // Close the Document, Without this → corrupted PDF
            document.close();
            // Return PDF as Byte Array...
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed To generate PDF");
        }
    }
}
