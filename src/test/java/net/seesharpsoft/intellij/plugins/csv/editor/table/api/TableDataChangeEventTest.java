package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class TableDataChangeEventTest {

    @Test
    public void instance_should_be_created_with_given_values() {
        Object[][] values = { { "Test", 1 }, { 2, 5} };
        TableDataChangeEvent event = new TableDataChangeEvent(this, values);

        assertThat(event.getSource()).isEqualTo(this);
        assertThat(event.getValue()).isEqualTo(values);
    }

    @Test
    public void getValue_should_return_copy_of_values() {
        Object[][] values = { { "Test", 1 }, { 2, 5} };
        TableDataChangeEvent event = new TableDataChangeEvent(this, values);

        assertThat(event.getValue()).isNotSameAs(values);
    }
}
