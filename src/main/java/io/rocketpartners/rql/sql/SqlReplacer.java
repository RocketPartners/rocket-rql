/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.rql.sql;

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.fluent.Term;

//
public class SqlReplacer
{
   public List<String> cols = new ArrayList();
   public List<String> vals = new ArrayList();

   public String replace(Term parent, Term leaf, int index, String col, String val)
   {
      if (val == null || val.trim().equalsIgnoreCase("null"))
         return "NULL";

      if (parent.hasToken("if") && index > 0)
      {
         if (SqlQuery.isNum(leaf))
            return val;
      }

      cols.add(col);
      vals.add(val);
      return "?";
   }
}