/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knopflerfish.bundle.repository_desktop;

import java.util.Set;

import javax.swing.table.DefaultTableModel;

import org.osgi.framework.Constants;

import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.knopflerfish.service.repositorymanager.RepositoryManager;

public class RepositoriesTableModel
  extends DefaultTableModel
{
  private static final long serialVersionUID = 7L;

  static final String[] COLUMN_NAMES =
    new String[] { "Description", "Rank", "Enabled" };

  private final RepositoryManager repoMgr;

  public RepositoriesTableModel(RepositoryManager repoMgr)
  {
    super(toArray(repoMgr), COLUMN_NAMES);
    this.repoMgr = repoMgr;
  }

  private static Object[][] toArray(RepositoryManager repoMgr)
  {
    final Set<RepositoryInfo> ris = repoMgr.getAllRepositories();

    final Object[][] data = new Object[ris.size()][3];
    int i = 0;
    for (final RepositoryInfo ri : ris) {
      data[i++][0] = ri;
    }
    return data;
  }

  /**
   * Apply all changes done via edits of the table.
   */
  public void applyChanges() {
    for (int row = 0; row< getRowCount(); row ++) {
      final RepositoryInfo ri = (RepositoryInfo) super.getValueAt(row, 0);
      for (int column = 1; column < getColumnCount(); column ++) {
        final Object newValue = super.getValueAt(row, column);
        if (newValue != null) {
          switch (column) {
          case 1:
            final int rank = ((Integer) newValue).intValue();
            repoMgr.setRepositoryRank(ri, rank);
            break;
          case 2:
            final boolean enabled = ((Boolean) newValue).booleanValue();
            repoMgr.setRepositoryEnabled(ri, enabled);
            break;
          }
        }
      }
    }
  }

  @Override
  public Class<?> getColumnClass(int columnIndex)
  {
    switch (columnIndex) {
    case 0:
      return String.class;
    case 1:
      return Integer.class;
    case 2:
      return Boolean.class;
    }
    return super.getColumnClass(columnIndex);
  }

  @Override
  public boolean isCellEditable(int row, int column)
  {
    return column > 0;
  }

  @Override
  public void setValueAt(Object aValue, int row, int column)
  {
    switch (column) {
    case 0:
      // Non-editable column; ignore new value.
      break;
    case 1:
        super.setValueAt(aValue, row, column);
    case 2:
      super.setValueAt(aValue, row, column);
    }
  }

  @Override
  public Object getValueAt(int row, int column)
  {
    final RepositoryInfo ri = (RepositoryInfo) super.getValueAt(row, 0);
    switch (column) {
    case 0:
      return ri.getProperty(Constants.SERVICE_DESCRIPTION);
    case 1:
      final Integer rank = (Integer) super.getValueAt(row, column);
      if (rank == null) {
        return new Integer(ri.getRank());
      } else {
        return rank;
      }
    case 2:
      final Boolean enabled = (Boolean) super.getValueAt(row, column);
      if (enabled == null) {
        return repoMgr.isEnabled(ri) ? Boolean.TRUE : Boolean.FALSE;
      } else {
        return enabled;
      }
    }
    return null;
  }

}
