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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.rocketpartners.fluent.Term;
import io.rocketpartners.rql.Group;
import io.rocketpartners.rql.Order;
import io.rocketpartners.rql.Order.Sort;
import io.rocketpartners.rql.Page;
import io.rocketpartners.rql.Query;
import io.rocketpartners.rql.Select;
import io.rocketpartners.rql.Where;

public class SqlQuery extends Query<SqlQuery, SqlQuery, Select<Select<Select, SqlQuery>, SqlQuery>, Where<Where<Where, SqlQuery>, SqlQuery>, Group<Group<Group, SqlQuery>, SqlQuery>, Order<Order<Order, SqlQuery>, SqlQuery>, Page<Page<Page, SqlQuery>, SqlQuery>>
{
   protected String    type        = null;
   String              selectSql   = null;

   protected char      stringQuote = '\'';
   protected char      columnQuote = '"';

   Map<String, String> propertyMap = new HashMap();

   public String toSql(SqlReplacer replacer)
   {
      Parts parts = new Parts(selectSql);

      StringBuffer cols = new StringBuffer();

      List<Term> terms = select().columns();
      for (int i = 0; i < terms.size(); i++)
      {
         Term term = terms.get(i);
         if (term.hasToken("as"))
         {
            Term function = term.getTerm(0);
            cols.append(" ").append(print(function, replacer, null));

            String colName = term.getToken(1);
            if (!(empty(colName) || colName.indexOf("$$$ANON") > -1))
            {
               cols.append(" AS " + asString(colName));
            }
         }
         else
         {
            cols.append(" " + asCol(term.getToken()));
         }

         if (i < terms.size() - 1)
            cols.append(", ");
      }

      if (cols.length() > 0)
      {
         boolean restrictCols = find("includes") != null;
         int star = parts.select.indexOf(" * ");
         if (restrictCols && star > 0)
         {
            parts.select = parts.select.substring(0, star + 1) + cols + parts.select.substring(star + 2, parts.select.length());
         }
         else
         {
            parts.select = parts.select.trim() + ", " + cols;
         }
      }

      if (select().isDistinct() && parts.select.toLowerCase().indexOf("distinct") < 0)
      {
         int idx = parts.select.toLowerCase().indexOf("select") + 6;
         parts.select = parts.select.substring(0, idx) + " DISTINCT " + parts.select.substring(idx, parts.select.length());
      }

      //      if (isCalcRowsFound() && stmt.pagenum > 0 && stmt.parts.select.toLowerCase().trim().startsWith("select"))
      //      {
      //         int idx = stmt.parts.select.toLowerCase().indexOf("select") + 6;
      //         stmt.parts.select = stmt.parts.select.substring(0, idx) + " SQL_CALC_FOUND_ROWS " + stmt.parts.select.substring(idx, stmt.parts.select.length());
      //      }

      terms = where().filters();
      for (int i = 0; i < terms.size(); i++)
      {
         Term term = terms.get(i);

         String where = print(term, replacer, null);
         if (where != null)
         {
            if (empty(parts.where))
               parts.where = " WHERE " + where;
            else
               parts.where += " AND " + where;
         }
      }

      Term groupBy = find("group");
      if (groupBy != null)
      {
         if (parts.group == null)
            parts.group = "GROUP BY ";

         for (Term group : groupBy.getTerms())
         {
            if (!parts.group.endsWith("GROUP BY "))
               parts.group += ", ";
            parts.group += asCol(group.getToken());
         }
      }

      List<Sort> sorts = order().getSorts();
      for (int i = 0; i < sorts.size(); i++)
      {
         //-- now setup the "ORDER BY" clause based on the
         //-- "sort" parameter.  Multiple sorts can be 
         //-- comma separated and a leading '-' indicates
         //-- descending sort for that field
         //--
         //-- ex: "sort=firstName,-age"

         Sort sort = sorts.get(i);
         if (parts.order == null)
            parts.order = "ORDER BY ";

         if (!parts.order.endsWith("ORDER BY "))
            parts.order += ", ";

         parts.order += asCol(sort.getProperty()) + (sort.isAsc() ? " ASC" : " DESC");
      }

      //-- now setup the LIMIT clause based
      //-- off of the  "offset" and "limit"
      //-- params OR the "page" and "pageSize"
      //-- query params.  

      int offset = page().getOffset();
      int limit = page().getLimit();

      parts.limit = this.buildLimitClause(offset, limit);

      //--compose the final statement
      String buff = parts.select;

      buff += " \r\n" + parts.from;

      if (parts.where != null)
         buff += " \r\n" + parts.where;

      if (parts.select.toLowerCase().startsWith("select "))
      {
         if (parts.group != null)
            buff += " \r\n" + parts.group;

         if (parts.order != null)
            buff += " \r\n" + parts.order;

         if (parts.limit != null)
            buff += " \r\n" + parts.limit;
      }

      return buff.toString();

   }

   protected String buildLimitClause(int offset, int limit)
   {
      String s = null;
      if (limit >= 0 || offset >= 0)
      {
         if ("postgres".equalsIgnoreCase(getType()) || "redshift".equalsIgnoreCase(getType()))
         {
            s = "";
            if (offset >= 0)
               s += "OFFSET " + offset;

            if (limit >= 0)
            {
               s += " LIMIT " + limit;
            }
         }
         else
         {
            s = "LIMIT ";
            if (offset >= 0)
               s += offset;

            if (limit >= 0)
            {
               if (!s.endsWith("LIMIT "))
                  s += ", ";

               s += limit;
            }
         }
      }
      return s;
   }

   protected String print(Term term, SqlReplacer replacer, String col)
   {
      if (term.isLeaf())
      {
         String token = term.getToken();

         if (isNum(term))
            return asNum(token);

         if (isCol(term))
            return asCol(token);

         return asString(term);
      }

      StringBuffer sql = new StringBuffer("");

      List<Term> terms = term.getTerms();
      String token = term.getToken();

      for (int i = 0; i < term.size(); i++)
      {
         Term child = term.getTerm(i);
         if (isCol(child))
         {
            col = child.token;
            break;
         }
      }

      List<String> strings = new ArrayList();
      for (Term t : terms)
      {
         strings.add(print(t, replacer, col));
      }

      List<String> origionals = new ArrayList(strings);

      //allows for callers to substitute callable statement "?"s
      //and/or to account for data type conversion 
      if (replacer != null)
      {
         for (int i = 0; i < terms.size(); i++)
         {
            Term t = terms.get(i);
            if (t.isLeaf())
            {
               String val = strings.get(i);
               if (val.charAt(0) != columnQuote)
               {
                  //val = t.getToken();//go back to the unprinted/quoted version
                  strings.set(i, replacer.replace(term, t, i, col, val));
               }
            }
         }
      }

      if (term.hasToken("eq", "ne", "like", "w", "sw", "ew"))
      {
         if (terms.size() > 2)
            sql.append("(");

         String string0 = strings.get(0);

         for (int i = 1; i < terms.size(); i++)
         {
            String stringI = strings.get(i);
            if ("null".equalsIgnoreCase(stringI))
            {
               if (term.hasToken("eq", "like", "w", "sw", "ew"))
               {
                  sql.append(string0).append(" IS NULL ");
               }
               else
               {
                  sql.append(string0).append(" IS NOT NULL ");
               }
            }
            else
            {
               boolean wildcard = origionals.get(i).indexOf('%') >= 0;

               if (wildcard)
               {
                  if (term.hasToken("ne"))
                     sql.append(string0).append(" NOT LIKE ").append(stringI);
                  else
                     sql.append(string0).append(" LIKE ").append(stringI);
               }
               else
               {
                  if (term.hasToken("eq"))
                     sql.append(string0).append(" = ").append(stringI);
                  else
                     sql.append(" NOT ").append(string0).append(" <=> ").append(stringI);
               }
            }

            if (i < terms.size() - 1)
               sql.append(" OR ").append(string0);
         }

         if (terms.size() > 2)
            sql.append(")");
      }
      else if ("nn".equalsIgnoreCase(token))
      {
         String term1 = strings.get(0);

         sql.append(term1).append(" IS NOT NULL ");
      }
      else if ("n".equalsIgnoreCase(token))
      {
         String term1 = strings.get(0);

         sql.append(term1).append(" IS NULL ");
      }
      else if ("lt".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" < ").append(strings.get(1));
      }
      else if ("le".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" <= ").append(strings.get(1));
      }
      else if ("gt".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" > ").append(strings.get(1));
      }
      else if ("ge".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" >= ").append(strings.get(1));
      }
      else if ("in".equalsIgnoreCase(token) || "out".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0));

         if ("out".equalsIgnoreCase(token))
            sql.append(" NOT");

         sql.append(" IN(");
         for (int i = 1; i < strings.size(); i++)
         {
            sql.append(strings.get(i));
            if (i < strings.size() - 1)
               sql.append(", ");
         }
         sql.append(")");
      }
      else if ("if".equalsIgnoreCase(token))
      {
         sql.append("IF(").append(strings.get(0)).append(", ").append(strings.get(1)).append(", ").append(strings.get(2)).append(")");
      }
      else if ("and".equalsIgnoreCase(token) || "or".equalsIgnoreCase(token))
      {
         sql.append("(");
         for (int i = 0; i < strings.size(); i++)
         {
            sql.append(strings.get(i).trim());
            if (i < strings.size() - 1)
               sql.append(" ").append(token.toUpperCase()).append(" ");
         }
         sql.append(")");
      }
      else if ("sum".equalsIgnoreCase(token) || "count".equalsIgnoreCase(token) || "min".equalsIgnoreCase(token) || "max".equalsIgnoreCase(token) || "distinct".equalsIgnoreCase(token))
      {
         String acol = strings.get(0);
         String s = token.toUpperCase() + "(" + acol + ")";
         sql.append(s);
      }

      else if ("miles".equalsIgnoreCase(token))
      {
         sql.append("point(").append(strings.get(0)).append(",").append(strings.get(1)).append(") <@> point(").append(strings.get(2)).append(",").append(strings.get(3)).append(")");
      }
      else
      {
         throw new RuntimeException("Unable to parse: " + term);
      }

      return sql.toString();
   }

   public SqlQuery withSelectSql(String selectSql)
   {
      this.selectSql = selectSql;
      return this;
   }

   public SqlQuery withType(String type)
   {
      this.type = type;
      return this;
   }

   public String getType()
   {
      return type;
   }

   public void withStringQuote(char stringQuote)
   {
      this.stringQuote = stringQuote;
   }

   public void withColumnQuote(char columnQuote)
   {
      this.columnQuote = columnQuote;
   }

   public SqlQuery withPropertyMap(String rqlName, String actualName)
   {
      propertyMap.put(rqlName.toLowerCase(), actualName);
      return this;
   }

   public SqlQuery withPropertyMap(Map<String, String> propertyMap)
   {
      for (String rqlName : propertyMap.keySet())
      {
         withPropertyMap(rqlName, propertyMap.get(rqlName));
      }
      return this;
   }

   public String getPropertyName(String rqlName)
   {
      return propertyMap.get(rqlName.toLowerCase());
   }

   protected boolean isCol(Term term)
   {
      if (!term.isLeaf())
         return false; //this is a function

      if (term.getQuote() == '"')//" is always the term identifier quote
         return true;

      if (term.getQuote() == '\'')
         return false; //this a string as specified by the user in the parsed rql

      if (isNum(term))
         return false;

      String token = term.getToken();
      if (propertyMap.containsKey(token.toLowerCase()))
         return true;

      if (term.getParent() != null && term.getParent().indexOf(term) == 0)
         return true;

      return false;
   }

   protected String asCol(String col)
   {
      if (propertyMap.containsKey(col.toLowerCase()))
         col = propertyMap.get(col.toLowerCase());

      return columnQuote + col.toString() + columnQuote;
   }

   protected String asString(String string)
   {
      return stringQuote + string + stringQuote;
   }

   protected String asString(Term term)
   {
      String token = term.token;
      Term parent = term.getParent();
      if (parent != null)
      {
         if (parent.hasToken("w"))
         {
            token = "*" + token + "*";
         }
         else if (parent.hasToken("sw"))
         {
            token = token + "*";
         }
         else if (parent.hasToken("ew"))
         {
            token = "*" + token;
         }

         if (parent.hasToken("eq", "ne", "w", "sw", "ew", "like"))
         {
            token = token.replace('*', '%');
         }
      }

      return stringQuote + token.toString() + stringQuote;
   }

   protected static String asNum(String token)
   {
      if ("true".equalsIgnoreCase(token))
         return "1";

      if ("false".equalsIgnoreCase(token))
         return "0";

      return token;
   }

   protected static boolean isNum(Term term)
   {
      if (!term.isLeaf() || term.isQuoted())
         return false;

      String token = term.getToken();
      try
      {
         Double.parseDouble(token);
         return true;
      }
      catch (Exception ex)
      {
         //not a number, ignore
      }

      if ("true".equalsIgnoreCase(token))
         return true;

      if ("false".equalsIgnoreCase(token))
         return true;

      if ("null".equalsIgnoreCase(token))
         return true;

      return false;
   }

   public class Parts
   {
      public String select = "";
      public String from   = "";
      public String where  = "";
      public String group  = "";
      public String order  = "";
      public String limit  = "";

      public Parts(String sql)
      {
         if (sql == null)
            return;

         select = chopFirst(sql, "select", "from");

         if (empty(select))
            select = chopFirst(sql, "update", "where");

         if (empty(select))
            select = chopFirst(sql, "delete", "from");

         if (select != null)
         {
            sql = sql.substring(select.length(), sql.length());

            if (sql.trim().substring(4).trim().startsWith("("))
            {
               int end = sql.lastIndexOf("as") + 3;
               String rest = sql.substring(end, sql.length());
               int[] otherIdx = findFirstOfFirstOccurances(rest, "where", " group by", "order", "limit");
               if (otherIdx != null)
               {
                  end += otherIdx[0];
               }
               else
               {
                  end = sql.length();
               }

               from = sql.substring(0, end);
               sql = sql.substring(end, sql.length());
            }
            else
            {
               from = chopLast(sql, "from", "where", "group by", "order", "limit");
            }
            where = chopLast(sql, "where", "group by", "order", "limit");
            group = chopLast(sql, "group by", "order", "limit");
            order = chopLast(sql, "order", "limit");
            limit = chopLast(sql, "limit");
         }
      }

      int[] findFirstOfFirstOccurances(String haystack, String... regexes)
      {
         int[] first = null;
         for (String regex : regexes)
         {
            regex = "\\b(" + regex.trim().replaceAll(" ", "\\\\s*") + ")\\b";
            Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(haystack);
            if (m.find())
            {
               int start = m.start(1);
               int end = m.end(1);

               if (first == null || start < first[0])
                  first = new int[]{start, end};
            }
         }
         //      if (first != null)
         //         System.out.println("found first '" + haystack.substring(first[0], first[1]) + "'");
         return first;
      }

      int[] findFirstOfLastOccurances(String haystack, String... regexes)
      {
         int[] first = null;

         for (String regex : regexes)
         {
            int[] last = null;

            regex = "\\b(" + regex.trim().replaceAll(" ", "\\\\s*") + ")\\b";
            Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(haystack);
            while (m.find())
            {
               int start = m.start(1);
               int end = m.end(1);

               if (last == null || start > last[0])
                  last = new int[]{start, end};
            }

            if (last != null && (first == null || last[0] < first[0]))
               first = last;
         }

         return first;
      }

      protected String chopLast(String haystack, String start, String... ends)
      {
         int[] startIdx = findFirstOfFirstOccurances(haystack, start);

         if (startIdx != null)
         {
            int[] endIdx = findFirstOfLastOccurances(haystack, ends);
            if (endIdx != null)
               return haystack.substring(startIdx[0], endIdx[0]).trim() + " ";
            else
               return haystack.substring(startIdx[0], haystack.length()).trim() + " ";
         }
         return null;
      }

      protected String chopFirst(String haystack, String start, String... ends)
      {
         int[] startIdx = findFirstOfFirstOccurances(haystack, start);

         if (startIdx != null)
         {
            int[] endIdx = findFirstOfFirstOccurances(haystack, ends);
            if (endIdx != null)
               return haystack.substring(startIdx[0], endIdx[0]).trim() + " ";
            else
               return haystack.substring(startIdx[0], haystack.length()).trim() + " ";
         }
         return null;
      }
   }
}
