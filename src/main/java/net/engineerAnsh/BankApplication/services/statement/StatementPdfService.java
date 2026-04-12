package net.engineerAnsh.BankApplication.services.statement;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Dto.Statements.StatementRowDto;
import net.engineerAnsh.BankApplication.Utils.CurrencyUtil;
import org.springframework.stereotype.Service;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class StatementPdfService {

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, int alignment, boolean noWrap) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setNoWrap(noWrap);
        return cell;
    }

    private String getGeneratedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

        return LocalDateTime.now()
                .atZone(ZoneId.of("Asia/Kolkata"))
                .format(formatter);
    }

    // It returns the PDF as a byte[] (perfect for REST API download), so it can be: downloaded, emailed, stored...
    public byte[] generatePdf(AccountStatementDto statement) {

        // Output Stream (Where PDF is written)...
        // Why this is needed: PDF is created in memory, Not saved to disk, It is :- Safe, fast, cloud-friendly
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Represents the PDF file itself, Everything you add goes inside this document...
        Document document = new Document(PageSize.A4);

        try {
            // Attach Writer to Document...
            PdfWriter.getInstance(document, out);

            // Open the Document
            document.open(); // Starts writing to the PDF, You must open before adding content...

            // Fonts:-
            BaseFont baseFont = BaseFont.createFont( // this works in locally, JAR, Docker, prod ...
                    Objects.requireNonNull(
                            getClass().getClassLoader()
                                    .getResource("fonts/NotoSans-Regular.ttf") // this will load the font that I saved in this path...
                    ).toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
            Font normalFont = new Font(baseFont, 10);
            Font amountFont = new Font(baseFont, 9);
            Font sectionFont = new Font(baseFont, 11, Font.BOLD);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font headerFont = new Font(baseFont, 10, Font.BOLD);

            // Header Section
            Paragraph title = new Paragraph("BANK OF ANSH", titleFont); // creating title with custom font
            title.setAlignment(Element.ALIGN_CENTER); // Set the title in the centre
            document.add(title); // adding the title in document...
            Paragraph subtitle = new Paragraph("Account Statement\n\n", sectionFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            // Account Information Section
            document.add(new Paragraph("Account Holder: " + statement.getAccountHolderName(), normalFont));
            document.add(new Paragraph("Account Number: " + statement.getMaskedAccountNumber(), normalFont));
            document.add(new Paragraph("Account Type: " + statement.getAccountType(), normalFont));
            document.add(new Paragraph("IFSC Code: " + statement.getIfscCode(),normalFont));
            document.add(new Paragraph("Statement Period: " + statement.getFromDate() + " to " + statement.getToDate(), normalFont));
            document.add(new Paragraph(
                    "Generated On: " + getGeneratedTimestamp(),
                    normalFont));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph(
                    "Opening Balance: " + CurrencyUtil.format(statement.getOpeningBalance()),
                    sectionFont
            ));

            // Create Table with 5 Columns : Date, Description, Debit, Credit, Balance...
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setSpacingBefore(15f); // spacing gap before table
            table.setWidths(new float[]{2.2f, 3.4f, 2.4f, 4.2f, 2.6f, 2.6f, 2.8f});

            // Add Table Headers...
            // This is the statement header rows.
            addHeaderCell(table, "Date", headerFont);
            addHeaderCell(table, "Txn Ref", headerFont);
            addHeaderCell(table, "Type", headerFont);
            addHeaderCell(table, "Description", headerFont);
            addHeaderCell(table, "Debit", headerFont);
            addHeaderCell(table, "Credit", headerFont);
            addHeaderCell(table, "Balance", headerFont);

            // Add Transaction Rows:-
            for (StatementRowDto row : statement.getTransactions()) {
                table.addCell(createCell(row.getDate().toString(),
                        normalFont,
                        Element.ALIGN_CENTER,
                        true)
                );
                table.addCell(createCell(row.getTransactionReference(),
                        normalFont,
                        Element.ALIGN_LEFT,
                        false)
                );
                table.addCell(createCell(row.getTransactionType(),
                        amountFont,
                        Element.ALIGN_LEFT,
                        false)
                );
                table.addCell(createCell(row.getDescription() != null ? row.getDescription() : "System Transaction",
                        normalFont,
                        Element.ALIGN_LEFT,
                        false));
                // If debit exists → show amount, Else → show '-'
                table.addCell(createCell(
                        row.getDebit() != null ? CurrencyUtil.format(row.getDebit()) : "-",
                        amountFont,
                        Element.ALIGN_RIGHT,
                        true));
                // If credit exists → show amount, Else → show '-'
                table.addCell(createCell(row.getCredit() != null ? CurrencyUtil.format(row.getCredit()) : "-",
                        amountFont,
                        Element.ALIGN_RIGHT,
                        true));
                table.addCell(createCell(CurrencyUtil.format(row.getBalance()), // Balance after the transaction...
                        amountFont,
                        Element.ALIGN_RIGHT,
                        true));
            }
            // Now the table becomes part of the PDF.
            document.add(table);

            // Footer Section...
            document.add(new Paragraph("\nClosing Balance: " +
                    CurrencyUtil.format(statement.getClosingBalance()),
                    sectionFont)
            );
            document.add(new Paragraph(
                    "\nThis is a system generated statement and does not require a signature.\n",
                    normalFont)
            );
            // Close the Document, Without this → corrupted PDF
            document.close();
            // Return PDF as Byte Array...
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed To generate PDF", e);
        }
    }
}
