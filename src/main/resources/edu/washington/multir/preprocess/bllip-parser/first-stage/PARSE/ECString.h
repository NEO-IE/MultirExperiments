/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

#include <algorithm>      // headers are not included by default in current version of g++ -- MJ 25th July 2008
#include <cstdlib>
#include <cstring>

#ifndef MECSTRING_H
#define MECSTRING_H

#define ECS gnu

#if ECS == gnu
using namespace std;
#include <string>
#define ECString string
#else
#include <bstring.h>
#define ECString string
#endif

#endif	/* ! MECSTRING_H */
