/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example;

/**
 * Example of JavaDoc to AsciiDoc conversion
 * <p>
 *     We run the {@code javadoc} tool on this class to test conversion of JavaDoc comments to AsciiDoc. This
 *     paragraph has two sentences.
 * </p>
 * <p>
 *     A sentence with <code>foo</code>, <code>foo`</code>, <code>foo</code>bar. Another sentence with {@code foo},
 *     {@code foo`}, {@code foo}bar.
 * </p>
 * <p>
 *     We can use <strong>strong</strong> <em>emphasis</em> too, or we can use <b>bold</b> and <i>italic</i>.
 * </p>
 * <p>
 *     Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum blandit dictum sem, ornare posuere lorem
 *     convallis sit amet. Sed dui augue, faucibus ut nisi id, mollis euismod nibh. Donec lobortis luctus viverra. In
 *     orci ante, pretium et fringilla at, sagittis nec justo. Cras finibus lorem vel volutpat interdum. Sed laoreet
 *     libero eros, ac cursus nibh dapibus vitae. Integer ante lorem, rhoncus at tortor vel, congue accumsan lorem.
 *     In hac habitasse platea dictumst. Nunc luctus ornare justo. Etiam ut metus id tortor dignissim semper. Nam
 *     turpis dui, aliquet nec enim et, faucibus accumsan dui.
 *</p>
 * <p>
 *     Aenean tincidunt elit id posuere mattis. Fusce bibendum sapien sed risus ultricies, non molestie erat volutpat.
 *     Donec nisi felis, egestas eu lobortis id, vulputate nec felis. In at dui euismod, blandit nulla et, accumsan
 *     elit. Proin id venenatis dui. Suspendisse sit amet est ut neque tincidunt venenatis. Donec bibendum quis velit
 *     fermentum porttitor. Maecenas faucibus, eros sit amet maximus malesuada, turpis neque bibendum justo, eu
 *     vehicula justo metus a ipsum. In at ullamcorper ipsum. Quisque in vehicula erat. Proin vitae suscipit dui,
 *     rutrum hendrerit augue. Curabitur finibus feugiat elit.
 * </p>
 * <ol>
 *     <li>Item with a nested ordered list.
 *         <ol>
 *             <li>First nested item.</li>
 *             <li>Second nested item.</li>
 *         </ol>
 *     </li>
 *     <li>Item with a nested unordered list.
 *         <ul>
 *             <li>Unordered list item.</li>
 *         </ul>
 *     </li>
 *     <li>
 *         <p>
 *             Item with complex content
 *         </p>
 *         <p>
 *             Mauris suscipit velit nec ligula mattis, nec varius augue accumsan. Curabitur a dolor dui. Quisque
 *             congue facilisis est nec dignissim. Pellentesque egestas eleifend faucibus. Fusce imperdiet ligula a
 *             lectus fringilla varius. Sed malesuada porta vulputate. Sed vulputate purus nec nibh interdum
 *             convallis. Cras faucibus, dolor tempus lacinia vehicula, elit risus luctus libero, sed molestie nisi
 *             lorem sit amet enim. Integer vitae enim sagittis, malesuada lorem at, interdum tellus. Suspendisse
 *             potenti. Vestibulum ac nisi sit amet ex dictum suscipit. Nulla varius augue a velit tincidunt feugiat.
 *             Proin fringilla id leo ut dignissim. Vivamus eu tellus eget orci suscipit viverra. Donec sodales et
 *             arcu vel mollis.
 *         </p>
 *         <p>
 *             Praesent gravida auctor lectus quis interdum. Etiam semper mauris quis neque bibendum molestie.
 *             Maecenas a lacus nec risus pellentesque accumsan. Suspendisse dictum dui eleifend nibh facilisis, non
 *             consequat neque elementum. Donec scelerisque ultricies ipsum, pretium elementum ex pellentesque
 *             malesuada. Mauris egestas massa vitae sapien lobortis convallis. Donec feugiat, purus commodo
 *             consequat vehicula, dolor urna aliquam arcu, id rutrum quam tortor quis libero. Sed varius justo eget
 *             congue lacinia.
 *         </p>
 *     </li>
 * </ol>
 * <ul>
 *     <li>Item of an unordered list.</li>
 * </ul>
 * <h2>First section</h2>
 * <table>
 *     <tr>
 *         <th>Key</th>
 *         <th>Value</th>
 *     </tr>
 *     <tr>
 *         <td>A</td>
 *         <td>1</td>
 *     </tr>
 *     <tr>
 *         <td>B</td>
 *         <td>2</td>
 *     </tr>
 * </table>
 * <h3>Subsection</h3>
 * <pre>
 *     private static final Logger logger = LogManager.getLogger();
 * </pre>
 */
public class JavadocExample {}
