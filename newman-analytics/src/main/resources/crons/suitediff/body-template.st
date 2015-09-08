<html>
    <body>
        <div><p>
          Summary:
          $if(summary.increasingDiff)$<font color=red>(+$summary.totalIncreasingDiff$ &uarr;)</font>$endif$
          $if(summary.decreasingDiff)$<font color=green>($summary.totalDecreasingDiff$ &darr;)</font>$endif$
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
        <div>
        <p></p>
        <table border=0>
            <tr><td>latest ($latestBuildBranch$)</td><td><a href="$latestUrl$">$latestBuildName$</a></td><td>$latestBuildDate$</td><td>&#9716; $latestBuildDuration$</td></tr>
            <tr><td>previous ($previousBuildBranch$)</td><td><a href="$previousUrl$">$previousBuildName$</a></td><td>$previousBuildDate$</td><td>&#9719; $previousBuildDuration$</td></tr>
        </table>
        </div>
   </body>
</html>