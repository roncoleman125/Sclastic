/*
 * Copyright (c) Sclastic Contributors
 * See CONTRIBUTORS.TXT for a full list of copyright holders.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Scaly Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE DEVELOPERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
  This program calculates Kendall's Tau (Conover 1980). It takes one input file
  in which the paired observations, x,y, (or in our case M, len) are sorted numerically
  by x then y.

  It was necessary to write this in C as Scala and AWK programs were far too slow and
  inefficient.

  To extract and sort the observations on the Sclastic report, run then script below:

  awk '/^\|/{print $2, $3}' report.txt |sort -n -k 1,1 -k 2,2 > kt-report.txt

  Add kt-report.txt as the first argument on the command line of this program.
*/

#include <stdio.h>
#include <stdlib.h>

#define MAX_OBSERVATIONS 224000
#define BUFSIZE 256

int main(int argc, char** argv) {
  if(argc <= 1) {
    printf("usage: kendall sorted-records-file\n");
    exit(1);
  }

  // Allocate storage
  int* x = (int*)malloc(MAX_OBSERVATIONS*sizeof(int));
  int* y = (int*)malloc(MAX_OBSERVATIONS*sizeof(int));

  // Read in the records
  char* line = malloc(BUFSIZE);
  FILE* fp = fopen(argv[1],"r");
  if(fp == NULL) {
    printf("file %s not found\n",argv[1]);
    exit(1);
  }

  size_t len = 0;
  ssize_t read;
  unsigned long indx = 0;
  while((read = getline(&line,&len,fp)) != -1) {
    printf("%s",line);
    int a, b;
    sscanf(line,"%d %d",&a, &b);
    //printf("%d %d\n",a,b);   
    x[indx] = a;
    y[indx] = b;
    indx++;

    if(indx >= MAX_OBSERVATIONS) {
      printf("storage overflow");
      exit(2);
    }
  }

  printf("processed %lu record(s)\n",indx);

  // Compute concordant and discordant counts
  // See https://www.youtube.com/watch?v=oXVxaSoY94k
  unsigned long nc = 0;
  unsigned long nd = 0;
  for(int i=0; i < indx; i++) {
    printf("%d\n",i);
    int xa = x[i];
    int ya = y[i];
    for(int j=i; j < indx; j++) {
      int xb = x[j];
      int yb = y[j];

      if(xa == xb)
        continue;

      if(yb > ya)
        nc++;

      if(yb < ya)
        nd++;
    }
  }

  printf("n =  %lu\n",indx);
  printf("Nc = %lu\n",nc);
  printf("Nd = %lu\n",nd);

  printf("Nc - Nd = %lu\n",nc-nd);

  double tau = (double) (nc - nd) / (indx * (indx - 1) / 2);
  printf("tau = %lf\n",tau);

  exit(0);
}

