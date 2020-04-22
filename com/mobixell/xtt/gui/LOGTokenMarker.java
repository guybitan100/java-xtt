package com.mobixell.xtt.gui;


import javax.swing.text.Segment;
/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: LOGTokenMarker.java,v 1.3 2008/03/20 13:36:28 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class LOGTokenMarker extends TokenMarker
{
    public static final String tantau_sccsid = "@(#)$Id: LOGTokenMarker.java,v 1.3 2008/03/20 13:36:28 rsoder Exp $";

   public LOGTokenMarker() {
   }

   public byte markTokensImpl(byte token, Segment line, int lineIndex) {
      char[] array = line.array;
      int offset = line.offset;
      int lastOffset = offset;
      int length = line.count + offset;

      for ( int i = offset; i < length; i++ ) {
         int ip1 = i+1;
         char c = array[i];
         switch ( token ) {
            case Token.NULL: // text
               switch ( c ) {
                  case 'E':
                     addToken(i-lastOffset, token);
                     lastOffset = i;
                     if ( ( lastOffset==0||SyntaxUtilities.regionMatches(false, line, lastOffset-1, "\n") ) && SyntaxUtilities.regionMatches(false, line, ip1, ": ") ) 
                     {
                        i += 2;
                        token = Token.COMMENT1;
                     }
                     else
                        token = Token.NULL;
                     break;
                  case 'F':
                     addToken(i-lastOffset, token);
                     lastOffset = i;
                     if ( ( lastOffset==0||SyntaxUtilities.regionMatches(false, line, lastOffset-1, "\n") ) && SyntaxUtilities.regionMatches(false, line, ip1, ": ") ) 
                     {
                        i += 2;
                        token = Token.KEYWORD1;
                     }
                     else
                        token = Token.NULL;
                     break;
                  case 'W':
                     addToken(i-lastOffset, token);
                     lastOffset = i;
                     if ( ( lastOffset==0||SyntaxUtilities.regionMatches(false, line, lastOffset-1, "\n") ) && SyntaxUtilities.regionMatches(false, line, ip1, ": ") ) 
                     {
                        i += 2;
                        token = Token.KEYWORD2;
                     }
                     else
                        token = Token.NULL;
                     break;
                  case 'I':
                     addToken(i-lastOffset, token);
                     lastOffset = i;
                     if ( ( lastOffset==0||SyntaxUtilities.regionMatches(false, line, lastOffset-1, "\n") ) && SyntaxUtilities.regionMatches(false, line, ip1, ": ") ) 
                     {
                        i += 2;
                        token = Token.KEYWORD3;
                     }
                     else
                        token = Token.NULL;
                     break;
               }
               break;

            case Token.COMMENT1:
               if (    SyntaxUtilities.regionMatches(false, line, i-1, "\nW: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nI: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nV: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nF: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nD: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nT: ")
                  )
               {
                  i=i-1;
                  addToken(ip1-1-lastOffset, token);
                  lastOffset = ip1-1;
                  token = Token.NULL;
               }
               break;

            case Token.KEYWORD2:
               if (    SyntaxUtilities.regionMatches(false, line, i-1, "\nD: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nT: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nV: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nF: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nE: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nI: ")
                  )
               {
                  i=i-1;
                  addToken(ip1-1-lastOffset, token);
                  lastOffset = ip1-1;
                  token = Token.NULL;
               }
               break;

            case Token.OPERATOR:
               break;

            case Token.LITERAL1:
            case Token.LITERAL2:
               break;

            case Token.LABEL: // entity
               break;

            case Token.KEYWORD1: // FAIL
               if (    SyntaxUtilities.regionMatches(false, line, i-1, "\nW: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nI: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nV: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nE: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nD: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nT: ")
                  )
               {
                  i=i-1;
                  addToken(ip1-1-lastOffset, token);
                  lastOffset = ip1-1;
                  token = Token.NULL;
               }
               break;

            case Token.COMMENT2:
               break;

            case Token.KEYWORD3:
               if (    SyntaxUtilities.regionMatches(false, line, i-1, "\nD: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nT: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nV: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nW: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nE: ")
                    || SyntaxUtilities.regionMatches(false, line, i-1, "\nF: ")
                  )
               {
                  i=i-1;
                  addToken(ip1-1-lastOffset, token);
                  lastOffset = ip1-1;
                  token = Token.NULL;
               }
               break;

            default:
               throw new InternalError("Invalid state: " + token);
         }
      }

      switch ( token ) {
         case Token.LABEL:
            addToken(length-lastOffset, Token.INVALID);
            token = Token.NULL;
            break;

         default:
            addToken(length-lastOffset, token);
            break;
      }

      return token;
   }
}


