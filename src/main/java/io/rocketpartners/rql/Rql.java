/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.rocketpartners.rql;

import java.util.HashMap;

public class Rql
{
   static HashMap<String, Rql> RQLS = new HashMap();

   public static void addRql(Rql rql)
   {
      RQLS.put(rql.getType().toLowerCase(), rql);
   }

   public static Rql getRql(String type)
   {
      return RQLS.get(type.toLowerCase());
   }

   protected String type = null;

   protected Rql(String type)
   {
      this.type = type;
   }

   public String getType()
   {
      return type;
   }

}
