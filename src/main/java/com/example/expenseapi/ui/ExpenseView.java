package com.example.expenseapi.ui;

import com.example.expenseapi.model.Expense;
import com.example.expenseapi.service.ExpenseService;
import com.example.expenseapi.service.ReceiptExtractorService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

@Route("")
public class ExpenseView extends HorizontalLayout {

    private final ExpenseService service;
    private final ReceiptExtractorService extractor;

    private final Grid<Expense> grid      = new Grid<>(Expense.class, false);
    private final TextField titleField    = new TextField();
    private final TextField amountField   = new TextField();
    private final TextField categoryField = new TextField();
    private final Button saveButton       = new Button("Add Entry");
    private final Button cancelButton     = new Button("Cancel");
    private final Binder<Expense> binder  = new Binder<>(Expense.class);
    private Expense editingExpense        = null;

    // Receipt scan UI
    private final MemoryBuffer uploadBuffer = new MemoryBuffer();
    private final Upload receiptUpload      = new Upload(uploadBuffer);
    private final Span  scanStatus          = new Span("or scan a receipt to auto-fill →");
    private final ProgressBar scanProgress  = new ProgressBar();

    // Sidebar stats
    private final Span totalLabel = new Span("KES 0.00");
    private final Span entryCount = new Span("0 entries");

    // ── CSS ──────────────────────────────────────────────────────────────────
    private static final String CSS = """
        @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@300;400;500&family=IBM+Plex+Sans:wght@300;400;500;600;700&display=swap');

        html {
            --lumo-primary-color:         #0f0f0f !important;
            --lumo-primary-text-color:    #0f0f0f !important;
            --lumo-primary-color-50pct:   rgba(15,15,15,0.5) !important;
            --lumo-primary-color-10pct:   rgba(15,15,15,0.07) !important;
            --lumo-error-color:           #6b6b6b !important;
            --lumo-error-text-color:      #6b6b6b !important;
            --lumo-success-color:         #4a4a4a !important;
            --lumo-base-color:            #ffffff !important;
            --lumo-body-text-color:       #0f0f0f !important;
            --lumo-secondary-text-color:  #6b6b6b !important;
            --lumo-disabled-text-color:   #c4c4c4 !important;
            --lumo-contrast-5pct:         rgba(0,0,0,0.03) !important;
            --lumo-contrast-10pct:        rgba(0,0,0,0.07) !important;
            --lumo-contrast-20pct:        rgba(0,0,0,0.13) !important;
            --lumo-contrast-60pct:        rgba(0,0,0,0.52) !important;
            --lumo-contrast-90pct:        rgba(0,0,0,0.88) !important;
            --lumo-contrast:              #0f0f0f !important;
            --lumo-tint-5pct:             rgba(0,0,0,0.03) !important;
            --lumo-tint-10pct:            rgba(0,0,0,0.06) !important;
            --lumo-font-family:           'IBM Plex Sans', sans-serif !important;
        }
        html, body { background: #f4f4f4 !important; margin: 0; padding: 0; }

        .sidebar {
            width: 220px; min-width: 220px; height: 100vh;
            background: #0f0f0f; display: flex; flex-direction: column;
            padding: 0; box-sizing: border-box; position: sticky; top: 0; overflow: hidden;
        }
        .sidebar-brand { padding: 24px 24px 20px; border-bottom: 1px solid #252525; }
        .sidebar-brand-name {
            font-family: 'IBM Plex Sans', sans-serif; font-size: 13px; font-weight: 700;
            color: #ffffff; letter-spacing: 0.1em; text-transform: uppercase; display: block;
        }
        .sidebar-section-label {
            font-family: 'IBM Plex Mono', monospace; font-size: 9px; font-weight: 500;
            letter-spacing: 0.22em; text-transform: uppercase; color: #777;
            padding: 20px 24px 8px; display: block;
        }
        .sidebar-nav-item {
            display: flex; align-items: center; gap: 10px; padding: 10px 24px;
            font-family: 'IBM Plex Sans', sans-serif; font-size: 13px; font-weight: 400;
            color: #c0c0c0; cursor: pointer; transition: background 0.1s, color 0.1s;
            border-left: 2px solid transparent;
        }
        .sidebar-nav-item.active { color: #ffffff; background: #1c1c1c; border-left-color: #ffffff; }
        .sidebar-nav-item:hover:not(.active) { color: #d0d0d0; background: #181818; }
        .sidebar-nav-dot { width: 5px; height: 5px; border-radius: 50%; background: currentColor; flex-shrink: 0; }
        .sidebar-stat-block { margin-top: auto; border-top: 1px solid #252525; padding: 20px 24px; }
        .sidebar-stat-label {
            font-family: 'IBM Plex Mono', monospace; font-size: 9px; letter-spacing: 0.2em;
            text-transform: uppercase; color: #888; display: block; margin-bottom: 6px;
        }
        .sidebar-stat-value {
            font-family: 'IBM Plex Sans', sans-serif; font-size: 20px; font-weight: 700;
            color: #ffffff; letter-spacing: -0.02em; display: block; line-height: 1.15;
        }
        .sidebar-stat-sub { font-family: 'IBM Plex Mono', monospace; font-size: 10px; color: #888; display: block; margin-top: 4px; }

        .main-content {
            flex: 1; min-height: 100vh; background: #f4f4f4;
            overflow-y: auto; overflow-x: hidden; box-sizing: border-box; min-width: 0;
        }
        .content-topbar {
            background: #ffffff; border-bottom: 1px solid #e4e4e4;
            padding: 0 32px; height: 52px;
            display: flex; align-items: center;
            position: sticky; top: 0; z-index: 10; box-sizing: border-box;
        }
        .topbar-title {
            font-family: 'IBM Plex Sans', sans-serif; font-size: 13px;
            font-weight: 600; color: #0f0f0f; letter-spacing: 0.02em;
        }
        .content-body { padding: 28px 32px 60px; box-sizing: border-box; width: 100%; }
        .section-header { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 12px; }
        .section-title {
            font-family: 'IBM Plex Sans', sans-serif; font-size: 10px; font-weight: 600;
            letter-spacing: 0.16em; text-transform: uppercase; color: #b0b0b0;
        }

        .form-card {
            background: #ffffff; border: 1px solid #e4e4e4;
            padding: 24px 28px; margin-bottom: 24px; width: 100%; box-sizing: border-box;
        }

        .scan-strip {
            display: flex; align-items: center; gap: 14px;
            padding: 14px 18px; background: #f9f9f9;
            border: 1px dashed #e0e0e0; margin-bottom: 20px;
            flex-wrap: wrap;
        }
        .scan-icon {
            font-size: 20px; line-height: 1; flex-shrink: 0;
        }
        .scan-label {
            font-family: 'IBM Plex Sans', sans-serif; font-size: 12px;
            font-weight: 600; color: #0f0f0f; white-space: nowrap;
        }
        .scan-hint {
            font-family: 'IBM Plex Mono', monospace; font-size: 11px; color: #aaa;
        }
        .scan-status {
            font-family: 'IBM Plex Mono', monospace; font-size: 11px; color: #888;
            margin-left: auto;
        }
        .scan-status.scanning { color: #555; font-style: italic; }
        .scan-status.success  { color: #2a7a2a; }
        .scan-status.error    { color: #8a3a3a; }

        vaadin-upload {
            padding: 0 !important; border: none !important; background: transparent !important;
        }
        vaadin-upload::part(drop-label) { display: none !important; }
        vaadin-upload-file-list { display: none !important; }

        vaadin-text-field::part(label) {
            font-family: 'IBM Plex Sans', sans-serif !important; font-size: 11px !important;
            font-weight: 500 !important; letter-spacing: 0.04em !important;
            color: #6b6b6b !important; padding-bottom: 5px !important;
        }
        vaadin-text-field::part(input-field) {
            background: #fafafa !important; border-radius: 0 !important;
            border: 1px solid #e0e0e0 !important; box-shadow: none !important;
            padding: 0 10px !important; height: 38px !important; transition: border-color 0.12s !important;
        }
        vaadin-text-field::part(input-field)::after { display: none !important; }
        vaadin-text-field:focus-within::part(input-field) {
            border-color: #0f0f0f !important; background: #ffffff !important; box-shadow: none !important;
        }
        vaadin-text-field[invalid]::part(input-field) { border-color: #999 !important; }
        vaadin-text-field input {
            color: #0f0f0f !important; font-family: 'IBM Plex Sans', sans-serif !important;
            font-size: 13px !important; caret-color: #0f0f0f !important;
        }
        vaadin-text-field input::placeholder {
            color: #c8c8c8 !important; font-family: 'IBM Plex Sans', sans-serif !important;
            font-size: 13px !important; opacity: 1 !important;
        }
        vaadin-text-field::part(error-message) {
            font-family: 'IBM Plex Mono', monospace !important; font-size: 10px !important; color: #8a8a8a !important;
        }
        vaadin-text-field[required]::part(required-indicator)::after { color: #ccc !important; }

        .btn-primary {
            font-family: 'IBM Plex Sans', sans-serif !important; font-size: 12px !important;
            font-weight: 600 !important; letter-spacing: 0.04em !important;
            color: #ffffff !important; background: #0f0f0f !important;
            border: 1px solid #0f0f0f !important; border-radius: 0 !important;
            height: 38px !important; min-width: 100px !important; padding: 0 18px !important;
            cursor: pointer !important; box-shadow: none !important;
            transition: background 0.1s !important; white-space: nowrap !important;
        }
        .btn-primary:hover { background: #2a2a2a !important; border-color: #2a2a2a !important; }
        .btn-ghost {
            font-family: 'IBM Plex Sans', sans-serif !important; font-size: 12px !important;
            font-weight: 500 !important; letter-spacing: 0.04em !important;
            color: #8a8a8a !important; background: transparent !important;
            border: 1px solid #e0e0e0 !important; border-radius: 0 !important;
            height: 38px !important; min-width: 80px !important; padding: 0 14px !important;
            cursor: pointer !important; box-shadow: none !important;
            transition: border-color 0.1s, color 0.1s !important; white-space: nowrap !important;
        }
        .btn-ghost:hover { border-color: #aaa !important; color: #333 !important; }

        vaadin-progress-bar {
            height: 2px !important; border-radius: 0 !important;
            --lumo-primary-color: #0f0f0f !important;
        }

        .table-card {
            background: #ffffff; border: 1px solid #e4e4e4;
            width: 100%; box-sizing: border-box; overflow: hidden;
        }
        vaadin-grid.corp-grid {
            background: #ffffff !important; border-radius: 0 !important;
            border: none !important; width: 100% !important;
        }
        vaadin-grid.corp-grid::part(header-cell) {
            background: #fafafa !important; border-bottom: 1px solid #e4e4e4 !important; padding: 11px 16px !important;
        }
        vaadin-grid.corp-grid::part(cell) {
            background: #ffffff !important; border-bottom: 1px solid #f0f0f0 !important;
            font-family: 'IBM Plex Sans', sans-serif !important; font-size: 13px !important;
            color: #1a1a1a !important; padding: 12px 16px !important;
        }
        vaadin-grid.corp-grid::part(row):hover vaadin-grid-cell-content { background: #fafafa !important; }
        .row-btn {
            background: transparent !important; border: 1px solid #e8e8e8 !important;
            border-radius: 0 !important; width: 28px !important; height: 28px !important;
            min-width: 28px !important; padding: 0 !important; cursor: pointer !important;
            box-shadow: none !important; color: #c0c0c0 !important; transition: border-color 0.1s, color 0.1s !important;
        }
        .row-btn:hover { border-color: #0f0f0f !important; color: #0f0f0f !important; }
        vaadin-notification-card {
            font-family: 'IBM Plex Sans', sans-serif !important; font-size: 12px !important; border-radius: 0 !important;
        }
    """;

    public ExpenseView(ExpenseService service, ReceiptExtractorService extractor) {
        this.service   = service;
        this.extractor = extractor;

        Div injector = new Div();
        injector.getElement().executeJs(
            "const s=document.createElement('style');s.textContent=$0;document.head.appendChild(s);", CSS
        );

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "#f4f4f4");

        add(injector, buildSidebar(), buildMainContent());

        configureForm();
        configureReceiptUpload();
        updateGrid();
        updateTotal();
    }

    private Div buildSidebar() {
        Div sidebar = new Div();
        sidebar.addClassName("sidebar");

        Div brand = new Div();
        brand.addClassName("sidebar-brand");
        Span brandName = new Span("FinanceOS");
        brandName.addClassName("sidebar-brand-name");
        brand.add(brandName);

        Span navLabel = new Span("Workspace");
        navLabel.addClassName("sidebar-section-label");

        Div statBlock = new Div();
        statBlock.addClassName("sidebar-stat-block");
        Span statLbl = new Span("Total Spent");
        statLbl.addClassName("sidebar-stat-label");
        totalLabel.addClassName("sidebar-stat-value");
        entryCount.addClassName("sidebar-stat-sub");
        statBlock.add(statLbl, totalLabel, entryCount);

        sidebar.add(brand, navLabel,
                buildNavItem("Expenses", true),
                buildNavItem("Reports", false),
                buildNavItem("Settings", false),
                statBlock);
        return sidebar;
    }

    private Div buildNavItem(String label, boolean active) {
        Div item = new Div();
        item.addClassName("sidebar-nav-item");
        if (active) item.addClassName("active");
        Div dot = new Div();
        dot.addClassName("sidebar-nav-dot");
        item.add(dot, new Span(label));
        return item;
    }

    private VerticalLayout buildMainContent() {
        VerticalLayout main = new VerticalLayout();
        main.addClassName("main-content");
        main.setPadding(false);
        main.setSpacing(false);

        Div topbar = new Div();
        topbar.addClassName("content-topbar");
        Span topbarTitle = new Span("Expense Management");
        topbarTitle.addClassName("topbar-title");
        topbar.add(topbarTitle);

        Div body = new Div();
        body.addClassName("content-body");

        configureGrid();
        Div tableCard = new Div();
        tableCard.addClassName("table-card");
        tableCard.add(grid);

        Div tableHeader = new Div();
        tableHeader.addClassName("section-header");
        Span tableTitle = new Span("All Entries");
        tableTitle.addClassName("section-title");
        tableHeader.add(tableTitle);

        body.add(buildFormCard(), tableHeader, tableCard);
        main.add(topbar, body);
        return main;
    }

    private Div buildFormCard() {
        Div card = new Div();
        card.addClassName("form-card");
        Div cardHeader = new Div();
        cardHeader.addClassName("section-header");
        Span sectionLbl = new Span("New Entry");
        sectionLbl.addClassName("section-title");
        sectionLbl.setId("xp-section-lbl");
        cardHeader.add(sectionLbl);


        Div scanStrip = new Div();
        scanStrip.addClassName("scan-strip");

        Span scanIcon = new Span("🧾");
        scanIcon.addClassName("scan-icon");

        Div scanText = new Div();
        Span scanLabel = new Span("Scan Receipt");
        scanLabel.addClassName("scan-label");
        Span scanHint = new Span("Upload a photo or image of your receipt — fields will be filled automatically");
        scanHint.addClassName("scan-hint");
        scanText.add(scanLabel, new Div(), scanHint);

        receiptUpload.setMaxFiles(1);
        receiptUpload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        receiptUpload.setMaxFileSize(10 * 1024 * 1024);
        Button uploadBtn = new Button("Choose image");
        uploadBtn.addClassName("btn-ghost");
        receiptUpload.setUploadButton(uploadBtn);
        receiptUpload.setDropLabel(new Span(""));

        scanStatus.addClassName("scan-status");

        scanProgress.setIndeterminate(true);
        scanProgress.setVisible(false);
        scanProgress.setWidth("100%");

        scanStrip.add(scanIcon, scanText, receiptUpload, scanStatus);
        titleField.setLabel("Description");
        titleField.setPlaceholder("What did you spend on?");
        titleField.getStyle().set("flex", "2").set("min-width", "140px");

        amountField.setLabel("Amount (KES)");
        amountField.setPlaceholder("0.00");
        amountField.getStyle().set("flex", "1").set("min-width", "100px");

        categoryField.setLabel("Category");
        categoryField.setPlaceholder("e.g. Food");
        categoryField.getStyle().set("flex", "1").set("min-width", "100px");

        saveButton.addClassName("btn-primary");
        cancelButton.addClassName("btn-ghost");
        cancelButton.setVisible(false);
        saveButton.addClickListener(e -> saveExpense());
        cancelButton.addClickListener(e -> clearForm());

        HorizontalLayout fields = new HorizontalLayout(
                titleField, amountField, categoryField, saveButton, cancelButton);
        fields.setAlignItems(Alignment.END);
        fields.setFlexGrow(1, titleField);
        fields.getStyle().set("gap", "12px").set("flex-wrap", "wrap").set("width", "100%");
        fields.setPadding(false);
        fields.setSpacing(false);

        card.add(cardHeader, scanStrip, scanProgress, fields);
        return card;
    }

    private void configureReceiptUpload() {
        receiptUpload.addStartedListener(event -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                scanProgress.setVisible(true);
                scanStatus.setText("Scanning receipt...");
                scanStatus.setClassName("scan-status scanning");
                saveButton.setEnabled(false);
            }));
        });

        receiptUpload.addSucceededListener(event -> {
            String mediaType = event.getMIMEType();
            getUI().ifPresent(ui -> ui.access(() -> {
                try {
                    byte[] imageBytes = uploadBuffer.getInputStream().readAllBytes();
                    ReceiptExtractorService.ExtractedExpense extracted =
                            extractor.extract(imageBytes, mediaType);
                    titleField.setValue(extracted.title() != null ? extracted.title() : "");
                    if (extracted.amount() != null) {
                        amountField.setValue(String.valueOf(extracted.amount()));
                    }
                    categoryField.setValue(extracted.category() != null ? extracted.category() : "");

                    scanStatus.setText("✓ Fields filled from receipt");
                    scanStatus.setClassName("scan-status success");
                    scanProgress.setVisible(false);
                    saveButton.setEnabled(true);

                    notify("Receipt scanned — please review and confirm", false);

                } catch (IOException e) {
                    scanStatus.setText("Could not read image");
                    scanStatus.setClassName("scan-status error");
                    scanProgress.setVisible(false);
                    saveButton.setEnabled(true);
                    notify("Failed to read receipt image", true);
                } catch (Exception e) {
                    scanStatus.setText("Scan failed — fill manually");
                    scanStatus.setClassName("scan-status error");
                    scanProgress.setVisible(false);
                    saveButton.setEnabled(true);
                    notify("Receipt scan failed: " + e.getMessage(), true);
                }
            }));
        });

        receiptUpload.addFileRejectedListener(event -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                scanStatus.setText("File rejected");
                scanStatus.setClassName("scan-status error");
                scanProgress.setVisible(false);
                saveButton.setEnabled(true);
                notify("File rejected: " + event.getErrorMessage(), true);
            }));
        });
    }

    private void configureGrid() {
        grid.addClassName("corp-grid");

        grid.addColumn(Expense::getTitle)
            .setHeader(colHdr("Description")).setFlexGrow(1).setResizable(true);

        grid.addColumn(expense -> {
            NumberFormat f = NumberFormat.getCurrencyInstance(new Locale("en", "KE"));
            f.setCurrency(Currency.getInstance("KES"));
            Double a = expense.getAmount();
            return f.format(a != null ? a : 0.0);
        }).setHeader(colHdr("Amount")).setWidth("140px").setFlexGrow(0);

        grid.addColumn(Expense::getCategory)
            .setHeader(colHdr("Category")).setWidth("130px").setFlexGrow(0);

        grid.addComponentColumn(expense -> {
            Button edit = new Button(VaadinIcon.PENCIL.create());
            edit.addClassName("row-btn");
            edit.getElement().setAttribute("aria-label", "Edit");
            edit.addClickListener(e -> editExpense(expense));

            Button del = new Button(VaadinIcon.TRASH.create());
            del.addClassName("row-btn");
            del.getElement().setAttribute("aria-label", "Delete");
            del.addClickListener(e -> {
                service.delete(expense.getId());
                updateGrid(); updateTotal();
                notify("Entry deleted", false);
            });

            HorizontalLayout acts = new HorizontalLayout(edit, del);
            acts.setPadding(false);
            acts.setSpacing(false);
            acts.getStyle().set("gap", "6px");
            return acts;
        }).setHeader(colHdr("Actions")).setAutoWidth(true).setFlexGrow(0);

        grid.setAllRowsVisible(true);
        grid.getStyle().set("width", "100%");
    }

    private Span colHdr(String txt) {
        Span s = new Span(txt);
        s.getStyle()
            .set("font-family", "'IBM Plex Sans', sans-serif")
            .set("font-size", "10px").set("font-weight", "600")
            .set("letter-spacing", "0.1em").set("text-transform", "uppercase")
            .set("color", "#b0b0b0");
        return s;
    }

    private void configureForm() {
        binder.forField(titleField).asRequired("Required").bind(Expense::getTitle, Expense::setTitle);
        binder.forField(amountField)
              .withNullRepresentation("")
              .withConverter(new StringToDoubleConverter("Must be a number"))
              .asRequired("Required")
              .bind(Expense::getAmount, Expense::setAmount);
        binder.forField(categoryField).bind(Expense::getCategory, Expense::setCategory);
        binder.setBean(new Expense());
    }

    private void editExpense(Expense expense) {
        editingExpense = expense;
        binder.readBean(expense);
        saveButton.setText("Update Entry");
        cancelButton.setVisible(true);
        grid.getElement().executeJs(
            "const l=document.getElementById('xp-section-lbl');if(l)l.textContent='Edit Entry';"
        );
    }

    private void saveExpense() {
        if (binder.validate().hasErrors()) { notify("Please fix the errors", true); return; }
        Expense expense = editingExpense != null ? editingExpense : new Expense();
        binder.writeBeanIfValid(expense);
        if (expense.getAmount() == null) expense.setAmount(0.0);
        boolean updating = editingExpense != null;
        service.save(expense);
        clearForm(); updateGrid(); updateTotal();
        notify(updating ? "Entry updated" : "Entry added", false);
    }

    private void clearForm() {
        binder.readBean(new Expense());
        editingExpense = null;
        saveButton.setText("Add Entry");
        saveButton.setEnabled(true);
        cancelButton.setVisible(false);
        scanStatus.setText("");
        scanStatus.setClassName("scan-status");
        scanProgress.setVisible(false);
        receiptUpload.clearFileList();
        grid.getElement().executeJs(
            "const l=document.getElementById('xp-section-lbl');if(l)l.textContent='New Entry';"
        );
    }

    private void updateGrid()  { grid.setItems(service.findAll()); }

    private void updateTotal() {
        var all = service.findAll();
        double t = all.stream().mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0).sum();
        NumberFormat f = NumberFormat.getCurrencyInstance(new Locale("en", "KE"));
        f.setCurrency(Currency.getInstance("KES"));
        totalLabel.setText(f.format(t));
        entryCount.setText(all.size() + (all.size() == 1 ? " entry" : " entries"));
    }

    private void notify(String msg, boolean err) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(err ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}