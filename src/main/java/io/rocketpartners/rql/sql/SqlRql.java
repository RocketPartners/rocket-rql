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

import java.util.Arrays;
import java.util.HashSet;

import io.rocketpartners.rql.Rql;

public class SqlRql extends Rql
{
   public static final HashSet<String> RESERVED = new HashSet(Arrays.asList(new String[]{"as", "includes", "sort", "order", "offset", "limit", "distinct", "aggregate", "function", "sum", "count", "min", "max"}));

   static
   {
      Rql.addRql(new SqlRql("mysql"));
      Rql.addRql(new SqlRql("postgresql"));
      Rql.addRql(new SqlRql("postgres"));
      Rql.addRql(new SqlRql("redshift"));
   }

   private SqlRql(String type)
   {
      super(type);
   }

   public String toSql(String select, String rql, SqlReplacer replacer) throws Exception
   {
      SqlQuery query = new SqlQuery();
      query.withTerms(rql);
      query.withSelectSql(select);

      if (type != null && type.toLowerCase().indexOf("mysql") > -1)
      {
         query.withColumnQuote('`');
         //setCalcRowsFound(true);
      }

      return query.toSql(replacer);
   }

}
