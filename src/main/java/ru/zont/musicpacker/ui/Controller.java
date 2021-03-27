package ru.zont.musicpacker.ui;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import ru.zont.musicpacker.MusicEntry;

import java.net.URL;
import java.util.ResourceBundle;

@SuppressWarnings("unchecked")
public class Controller implements Initializable {
    public Button bt_select;
    public TextField tf_name;
    public TextField tf_prefix;
    public VBox grp_operations;
    public Button bt_zip;
    public Button bt_dir;
    public TableView<MusicEntry> table;
    public Button bt_cfg_mc;
    public Button bt_cfg_m;
    public Button bt_select_f;
    public ProgressBar pb;
    public CheckBox cb_wrap;
    public VBox grp_top;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        table.setEditable(true);
        // allows the individual cells to be selected
        table.getSelectionModel().cellSelectionEnabledProperty().set(true);
        // when character or numbers pressed it will start edit in editable
        // fields
        table.setOnKeyPressed(event -> {
            if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
                editFocusedCell();
            } else if (event.getCode() == KeyCode.RIGHT
                    || event.getCode() == KeyCode.TAB) {
                table.getSelectionModel().selectNext();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                // work around due to
                // TableView.getSelectionModel().selectPrevious() due to a bug
                // stopping it from working on
                // the first column in the last row of the table
                selectPrevious();
                event.consume();
            }
        });

        setEditable(1);
        setEditable(0);
    }

    private void setEditable(int index) {
        TableColumn<MusicEntry, String> tableColumn = (TableColumn<MusicEntry, String>) table.getColumns().get(index);
        tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        tableColumn.setEditable(true);
    }

    private void editFocusedCell() {
        final TablePosition<MusicEntry, ?> focusedCell = table
                .focusModelProperty().get().focusedCellProperty().get();
        table.edit(focusedCell.getRow(), focusedCell.getTableColumn());
    }

    private void selectPrevious() {
        if (table.getSelectionModel().isCellSelectionEnabled()) {
            // in cell selection mode, we have to wrap around, going from
            // right-to-left, and then wrapping to the end of the previous line
            TablePosition<MusicEntry, ?> pos = table.getFocusModel()
                    .getFocusedCell();
            if (pos.getColumn() - 1 >= 0) {
                // go to previous row
                table.getSelectionModel().select(pos.getRow(),
                        getTableColumn(pos.getTableColumn(), -1));
            } else if (pos.getRow() < table.getItems().size()) {
                // wrap to end of previous row
                table.getSelectionModel().select(pos.getRow() - 1,
                        table.getVisibleLeafColumn(
                                table.getVisibleLeafColumns().size() - 1));
            }
        } else {
            int focusIndex = table.getFocusModel().getFocusedIndex();
            if (focusIndex == -1) {
                table.getSelectionModel().select(table.getItems().size() - 1);
            } else if (focusIndex > 0) {
                table.getSelectionModel().select(focusIndex - 1);
            }
        }
    }

    private TableColumn<MusicEntry, ?> getTableColumn(
            final TableColumn<MusicEntry, ?> column, int offset) {
        int columnIndex = table.getVisibleLeafIndex(column);
        int newColumnIndex = columnIndex + offset;
        return table.getVisibleLeafColumn(newColumnIndex);
    }
}
