<html>
    <body>
        <div><p>
          Summary:
          $if(summary.positiveDiff)$<font color=red>(+$summary.positiveDiff$ &uarr;)</font>$endif$
          $if(summary.negativeDiff)$<font color=green>($summary.negativeDiff$ &darr;)</font>$endif$
        </p></div>
        <div class="styling">
        <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
            <tr>
                <th>Suite</th>
                <th>Failures</th>
                <th>Total</th>
            </tr>
            $diffs:{
              <tr>
                <td>$it.suiteName$</td>
                <td>
                    $it.failedTests$
                    $if(it.increasingDiffFailedTests)$<font color=red>(+$it.diffFailedTests$ &uarr;)</font>
                    $elseif(it.decreasingDiffFailedTests)$<font color=green>($it.diffFailedTests$ &darr;)</font>
                    $endif$
                </td>
                <td>
                    $it.totalTests$
                    $if(it.increasingDiffTotalTests)$(+$it.diffTotalTests$ &uarr;)
                    $elseif(it.decreasingDiffTotalTests)$($it.diffTotalTests$ &darr;)
                    $endif$
                </td>
              </tr>
            }$
        </table>
        </div>
        <div><p>
        latest build: <a href="$latestUrl$">$latestBuildName$</a>
        <br>
        previous build: <a href="$previousUrl$">$previousBuildName$</a>
        </p></div>
   </body>
</html>