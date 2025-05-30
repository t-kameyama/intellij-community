/*
 * Copyright 2002-2005 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.xpathView.ui;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.ide.DataManager;
import com.intellij.javaee.ExternalResourceManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlElement;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.containers.BidirectionalMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.PlatformColors;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.xpath.XPathFileType;
import org.intellij.lang.xpath.context.*;
import org.intellij.lang.xpath.psi.XPathElement;
import org.intellij.plugins.xpathView.XPathBundle;
import org.intellij.plugins.xpathView.util.MyPsiUtil;
import org.intellij.plugins.xpathView.util.Namespace;
import org.intellij.plugins.xpathView.util.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.xml.namespace.QName;
import java.awt.*;
import java.util.*;
import java.util.List;

import static org.intellij.plugins.xpathView.util.Copyable.Util.copy;

public class EditContextDialog extends DialogWrapper {

  private final Set<String> myUnresolvedPrefixes;

  private final JBTable myVariableTable;
  private final VariableTableModel myVariableTableModel;

  private final JBTable myNamespaceTable;
  private final NamespaceTableModel myNamespaceTableModel;
  private final ContextProvider myContextProvider;
  private JBSplitter mySplitter;

  public EditContextDialog(Project project,
                           Set<String> unresolvedPrefixes,
                           Collection<Namespace> namespaces,
                           Collection<Variable> variables,
                           ContextProvider contextProvider) {
    super(project, false);
    myUnresolvedPrefixes = unresolvedPrefixes;

    setTitle(XPathBundle.message("dialog.title.edit.xpath.context"));
    setModal(true);

    myContextProvider = new MyContextProvider(contextProvider);

    final List<Variable> m = copy(variables);
    myVariableTableModel = new VariableTableModel(m, project, XPathFileType.XPATH);
    myVariableTable = new JBTable(myVariableTableModel);
    myVariableTable.setDefaultRenderer(String.class, new VariableCellRenderer(m));
    myVariableTable.setDefaultRenderer(Expression.class, new ExpressionCellRenderer(project));
    myVariableTable.setDefaultEditor(Expression.class, new ExpressionCellEditor(project));

    int width = new JBLabel(XPathBundle.message("label.name")).getPreferredSize().width;
    myVariableTable.getColumnModel().getColumn(0).setMinWidth(width);
    myVariableTable.getColumnModel().getColumn(0).setMaxWidth(width * 5);
    myVariableTable.setPreferredScrollableViewportSize(JBUI.size(200, 130));

    final List<Namespace> n = copy(namespaces);
    myNamespaceTableModel = new NamespaceTableModel(n);
    myNamespaceTable = new JBTable(myNamespaceTableModel);
    myNamespaceTable.setDefaultRenderer(String.class, new NamespaceCellRenderer(n));

    width = new JBLabel(XPathBundle.message("label.prefix")).getPreferredSize().width;
    myNamespaceTable.getColumnModel().getColumn(0).setMinWidth(width);
    myNamespaceTable.getColumnModel().getColumn(0).setMaxWidth(width * 4);
    myNamespaceTable.setPreferredScrollableViewportSize(JBUI.size(200, 150));

    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel p = ToolbarDecorator.createDecorator(myVariableTable)
      .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          stopVarEditing();
          myVariableTableModel.addVariable();
          myNamespaceTable.editCellAt(myVariableTableModel.getRowCount() - 1, 0);
        }
      }).setRemoveAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          myVariableTableModel.removeVariable(myVariableTable.getSelectedRow());
        }
      }).disableUpDownActions().createPanel();
    UIUtil.addBorder(p, IdeBorderFactory.createTitledBorder(XPathBundle.message("border.title.variables"), false));

    final JPanel n = ToolbarDecorator.createDecorator(myNamespaceTable)
      .setAddAction(myContextProvider.getContextElement() != null ? null : new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          final ExternalResourceManager erm = ExternalResourceManager.getInstance();
          final List<String> allURIs = new ArrayList<>(Arrays.asList(erm.getResourceUrls(null, true)));
          final Collection<Namespace> namespaces = myNamespaceTableModel.getNamespaces();
          for (Namespace namespace : namespaces) {
            allURIs.remove(namespace.getUri());
          }
          Collections.sort(allURIs);

          final DataContext dataContext = DataManager.getInstance().getDataContext(myNamespaceTable);
          final Project project = CommonDataKeys.PROJECT.getData(dataContext);
          final AddNamespaceDialog dlg = new AddNamespaceDialog(project, myUnresolvedPrefixes, allURIs, AddNamespaceDialog.Mode.EDITABLE);
          if (dlg.showAndGet()) {
            myNamespaceTableModel.addNamespace(new Namespace(dlg.getPrefix(), dlg.getURI()));
          }
        }
      }).setRemoveAction(
        myContextProvider.getContextElement() != null ? null : new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            myNamespaceTableModel.removeNamespace(myNamespaceTable.getSelectedRow());
          }
        }).disableUpDownActions().createPanel();
    UIUtil.addBorder(n, IdeBorderFactory.createTitledBorder(XPathBundle.message("border.title.namespaces"), false));

    mySplitter = new JBSplitter(true, getDimensionServiceKey(), 400 / 1000f);
    mySplitter.setHonorComponentsMinimumSize(true);
    mySplitter.setFirstComponent(n);
    mySplitter.setSecondComponent(p);

    return mySplitter;
  }

  @Override
  protected @NotNull String getDimensionServiceKey() {
    return getClass().getName() + ".DIMENSION_SERVICE_KEY";
  }

  public Pair<Collection<Namespace>, Collection<Variable>> getContext() {
    assert isOK();

    return Pair.create(myNamespaceTableModel.getNamespaces(), myVariableTableModel.getVariables());
  }

  @Override
  protected void doOKAction() {
    stopVarEditing();
    stopNamespaceEditing();

    final List<Expression> expressions = myVariableTableModel.getExpressions();
    for (int i = 0; i < expressions.size(); i++) {
      final Expression expression = expressions.get(i);
      final String name = (String)myVariableTableModel.getValueAt(i, 0);

      final String expr = expression.getExpression();
      if ((expr == null || expr.trim().isEmpty()) && (name == null || name.trim().isEmpty())) {
        continue;
      }
      final String error = getError(expression);
      if (error != null) {
        Messages.showErrorDialog(expression.getFile().getProject(),
                                 XPathBundle.message("dialog.message.error.in.xpath.expression.for.variable", name, error),
                                 XPathBundle.message("dialog.title.xpath.error"));
        myVariableTable.getSelectionModel().setSelectionInterval(i, i);
        return;
      }
    }
    super.doOKAction();
  }

  private static String getError(final Expression expression) {
    return MyPsiUtil.checkFile(expression.getFile());
  }

  private void stopVarEditing() {
    final TableCellEditor cellEditor = myVariableTable.getCellEditor();
    if (cellEditor != null) {
      cellEditor.stopCellEditing();
    }
  }

  private void stopNamespaceEditing() {
    final TableCellEditor cellEditor = myNamespaceTable.getCellEditor();
    if (cellEditor != null) {
      cellEditor.stopCellEditing();
    }
  }

  private class VariableTableModel extends AbstractTableModel {
    private final Project myProject;
    private final LanguageFileType myFileType;

    private final List<Variable> myVariables;
    private final List<Expression> myList;

    VariableTableModel(List<Variable> variables, Project project, LanguageFileType fileType) {
      this.myVariables = variables;
      this.myProject = project;
      this.myFileType = fileType;

      myList = new ArrayList<>(variables.size());
      for (Variable variable : variables) {
        final Expression expression = Expression.create(project, fileType, variable.getExpression());
        myContextProvider.attachTo(expression.getFile());
        myList.add(expression);
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return columnIndex == 0 ? String.class : Expression.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return true;
    }

    @Override
    public String getColumnName(int column) {
      return column == 0 ? XPathBundle.message("name") : XPathBundle.message("expression");
    }

    @Override
    public int getRowCount() {
      return myVariables.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      final Variable variable = myVariables.get(rowIndex);
      if (columnIndex == 0) {
        return variable.getName();
      }
      else {
        return myList.get(rowIndex);
      }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (rowIndex >= myVariables.size()) {
        return;
      }
      final Variable variable = myVariables.get(rowIndex);
      if (columnIndex == 0) {
        variable.setName((String)aValue);
      }
      else {
        variable.setExpression(((Expression)aValue).getExpression());
      }
      fireTableDataChanged();
    }

    public Collection<Variable> getVariables() {
      return myVariables;
    }

    public List<Expression> getExpressions() {
      return myList;
    }

    public void addVariable() {
      final Variable variable = new Variable();
      myVariables.add(variable);
      final Expression expression = Expression.create(myProject, myFileType);
      myContextProvider.attachTo(expression.getFile());
      myList.add(expression);

      final int firstRow = myVariables.size() - 1;
      fireTableRowsInserted(firstRow, firstRow);
    }

    public void removeVariable(int selectedRow) {
      myVariables.remove(selectedRow);
      myList.remove(selectedRow);
      fireTableRowsDeleted(selectedRow, selectedRow);
    }
  }

  private static class VariableCellRenderer extends DefaultTableCellRenderer {
    private final List<Variable> myVariables;

    VariableCellRenderer(List<Variable> variables) {
      this.myVariables = variables;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object _value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, _value, isSelected, hasFocus, row, column);
      setForeground(table.getForeground());
      setToolTipText(null);

      for (int i = 0; i < myVariables.size(); i++) {
        Variable variable = myVariables.get(i);
        if (i != row && variable.getName().equals(_value)) {
          setForeground(JBColor.RED);
          setToolTipText(XPathBundle.message("tooltip.duplicate.variable"));
        }
        else if (variable.getExpression().isEmpty()) {
          setForeground(PlatformColors.BLUE);
          setToolTipText(XPathBundle.message("tooltip.empty.expression.variable.will.evaluate.to.empty.nodeset"));
        }
      }
      return this;
    }
  }

  private static class NamespaceTableModel extends AbstractTableModel {
    private final List<Namespace> myNamespaces;

    NamespaceTableModel(List<Namespace> namespaces) {
      this.myNamespaces = namespaces;
    }

    public Collection<Namespace> getNamespaces() {
      return myNamespaces;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0;
    }

    @Override
    public String getColumnName(int column) {
      return column == 0 ? XPathBundle.message("prefix") : XPathBundle.message("uri");
    }

    @Override
    public int getRowCount() {
      return myNamespaces.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      final Namespace namespace = myNamespaces.get(rowIndex);
      return columnIndex == 0 ? namespace.getPrefix() : namespace.getUri();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == 0) {
        final Namespace namespace = myNamespaces.get(rowIndex);
        namespace.setPrefix((String)aValue);
      }
    }

    public void addNamespace(Namespace namespace) {
      final int firstRow = myNamespaces.size();
      myNamespaces.add(namespace);
      fireTableRowsInserted(firstRow, firstRow);
    }

    public void removeNamespace(int selectedRow) {
      myNamespaces.remove(selectedRow);
      fireTableRowsDeleted(selectedRow, selectedRow);
    }
  }

  private static class NamespaceCellRenderer extends DefaultTableCellRenderer {
    private final List<Namespace> myNamespaces;

    NamespaceCellRenderer(List<Namespace> namespaces) {
      this.myNamespaces = namespaces;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setForeground(table.getForeground());

      final String prefix = myNamespaces.get(row).getPrefix();
      if (column == 1 && prefix == null || prefix.isEmpty()) {
        setForeground(PlatformColors.BLUE);
      }
      else if (column == 0) {
        for (int i = 0; i < myNamespaces.size(); i++) {
          Namespace namespace = myNamespaces.get(i);
          if (i != row && namespace.getPrefix().equals(value)) {
            setForeground(JBColor.RED);
            break;
          }
        }
      }
      return this;
    }
  }

  private class MyNamespaceContext implements NamespaceContext {
    @Override
    public @Nullable String getNamespaceURI(String prefix, XmlElement context) {
      return Namespace.makeMap(myNamespaceTableModel.getNamespaces()).get(prefix);
    }

    @Override
    public @Nullable String getPrefixForURI(String uri, XmlElement context) {
      final BidirectionalMap<String, String> bidiMap = new BidirectionalMap<>();
      bidiMap.putAll(Namespace.makeMap(myNamespaceTableModel.getNamespaces()));
      final List<String> list = bidiMap.getKeysByValue(uri);
      return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public @NotNull Collection<String> getKnownPrefixes(XmlElement context) {
      return Namespace.makeMap(myNamespaceTableModel.getNamespaces()).keySet();
    }

    @Override
    public @Nullable PsiElement resolve(String prefix, XmlElement context) {
      return null;
    }

    @Override
    public IntentionAction[] getUnresolvedNamespaceFixes(@NotNull PsiReference reference, String localName) {
      return IntentionAction.EMPTY_ARRAY;
    }

    @Override
    public String getDefaultNamespace(XmlElement context) {
      return null;
    }
  }

  private class MyVariableContext extends SimpleVariableContext {
    @Override
    public String @NotNull [] getVariablesInScope(XPathElement element) {
      final Collection<Variable> variables = myVariableTableModel.getVariables();
      return Variable.asSet(variables).toArray(new String[variables.size()]);
    }
  }

  private class MyContextProvider extends ContextProvider {
    private final MyNamespaceContext myNamespaceContext;
    private final MyVariableContext myVariableContext;
    private final ContextProvider myContextProvider;

    MyContextProvider(ContextProvider contextProvider) {
      myContextProvider = contextProvider;
      myNamespaceContext = new MyNamespaceContext();
      myVariableContext = new MyVariableContext();
    }

    @Override
    public @NotNull ContextType getContextType() {
      return myContextProvider.getContextType();
    }

    @Override
    public @Nullable XmlElement getContextElement() {
      return myContextProvider.getContextElement();
    }

    @Override
    public @Nullable NamespaceContext getNamespaceContext() {
      return myNamespaceContext;
    }

    @Override
    public @Nullable VariableContext getVariableContext() {
      return myVariableContext;
    }

    @Override
    public @Nullable Set<QName> getAttributes(boolean forValidation) {
      return myContextProvider.getAttributes(forValidation);
    }

    @Override
    public @Nullable Set<QName> getElements(boolean forValidation) {
      return myContextProvider.getElements(forValidation);
    }
  }
}
