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
package io.rocketpartners.rql;

import io.rocketpartners.fluent.Builder;
import io.rocketpartners.fluent.Parser;
import io.rocketpartners.fluent.Term;

/**
 * 
   //      query q = new Query().where('field').gt(100)
   //                           .where('ield2').lt(500)
   //                           .where("field>200")
   //                           .where("and(gt(field, 100)&lt(field2, 0))")
   //                           .where(or(gt("field", 5), lt("field", 2)))
   //                           .where().gt("field", 5).lt("field", 2)
 * @author wells
 *
 */
public class Query<T extends Query, P extends Builder, S extends Select, W extends Where, R extends Group, O extends Order, G extends Page> extends Builder<T, P>
{
   S select = null;
   W where  = null;
   R group  = null;
   O order  = null;
   G page   = null;

   //-- OVERRIDE ME TO ADD NEW FUNCTIONALITY --------------------------
   //------------------------------------------------------------------
   //------------------------------------------------------------------
   protected Parser createParser()
   {
      return new Parser();
   }

   protected S createSelect()
   {
      return (S) new Select(this);
   }

   protected W createWhere()
   {
      return (W) new Where(this);
   }

   protected R createGroup()
   {
      return (R) new Group(this);
   }

   protected O createOrder()
   {
      return (O) new Order(this);
   }

   protected G createPage()
   {
      return (G) new Page(this);
   }

   public T withTerm(Term term)
   {
      return super.withTerm(term);
   }

   //------------------------------------------------------------------
   //------------------------------------------------------------------

   public Query()
   {
      this(null);
   }

   public Query(String rql)
   {
      super(null);

      //order matters when multiple clauses can accept the same term 
      where();
      page();
      order();
      group();
      select();

      if (rql != null)
      {
         withTerms(rql);
      }
   }

   @Override
   public Parser getParser()
   {
      if (parser == null)
         parser = createParser();

      return parser;
   }

   public S select()
   {
      if (select == null)
      {
         select = createSelect();
         withBuilder(select);
      }
      return select;
   }

   public W where()
   {
      if (where == null)
      {
         where = createWhere();
         withBuilder(where);
      }
      return where;
   }

   public R group()
   {
      if (group == null)
      {
         group = createGroup();
         withBuilder(group);
      }
      return group;
   }

   public O order()
   {
      if (order == null)
      {
         order = createOrder();
         withBuilder(order);
      }
      return order;
   }

   public G page()
   {
      if (page == null)
      {
         page = createPage();
         withBuilder(page);
      }
      return page;
   }
}
