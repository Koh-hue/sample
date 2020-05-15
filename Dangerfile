# Ignore inline messages which lay outside a diff's range of PR
github.dismiss_out_of_range_messages

# Make it more obvious that a PR is a work in progress and shouldn't be merged yet
if github.pr_json["requested_reviewers"].length != 0 
  warn("レビューのリクエストはWIPを外してからしましょう。") if github.pr_title.include? "[WIP]"
end

# note when a pr's changes is extremely big 
is_big_pr = git.lines_of_code > 500
if is_big_pr
  warn("PRの変更量が多いので、可能であればPRを分割しましょう。")
end

# ensure there is summary
if github.pr_body.length < 5
  warn("プルリクの内容を記載しましょう。")
end

# note when prs dont reference a milestone
milestone = github.pr_json["milestone"]
has_milestone = milestone != nil
warn("マイルストーンを設定しましょう。", sticky: false) unless has_milestone
# make sure that where this branch is going to be merged
is_to_master = github.branch_for_base == 'master'
if is_to_master
  failure("masterブランチへ直接マージしないでください。")
end
# make sure that where branch comes from
is_from_develop = github.branch_for_base.include? "develop_ph2"
if has_milestone
  failure("Branchのベースがdevelop_ph2/#{milestone}か確認しましょう。") unless is_from_develop
else
  failure("Branchのベースがdevelop_ph2/*.*.*か確認しましょう。") unless is_from_develop
end

# note when a pr cannot be merged
can_merge = github.pr_json["mergeable"]
warn("このPRはまだマージできません。", sticky: false) unless can_merge

# warn when there are merge commits in the diff
if git.commits.any? { |c| c.message =~ /^Merge branch 'develop_ph2\/[0-9]+\.[0-9]+\.[0-9]'/ }
  warn("マージコミットがPRに含まれています。なるべく取り除きましょう。")
end

# highlight with a clickable link if a Project.xml is modified
if git.modified_files.include? ".idea/codeStyles/Project.xml"
  warn("#{github.html_link("Project.xml")}が編集されました。")
end

# public folder apk is uploaded
public_install_page_url = ENV['S3_APK_PATH']
message("📱 [New app deployed to Bitrise](#{public_install_page_url})") if public_install_page_url

# ktlint
# checkstyle_format.base_path = Dir.pwd
# checkstyle_format.report 'app/build/reports/ktlint/ktlint-main.xml'
