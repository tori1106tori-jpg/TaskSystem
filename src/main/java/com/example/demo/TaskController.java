package com.example.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TaskController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // ★【バリデーション＆認証強化版】先生の要求通り、未入力チェックとDB照合を完全実装しました
    @PostMapping("/login")
    public String login(
            @RequestParam(value = "username", required = false) String username, 
            @RequestParam(value = "password", required = false) String password, 
            Model model) {
        
        // 1. バリデーションチェック（未入力・空白のチェック）
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("errorMessage", "ユーザー名を入力してください。");
            return "index"; // ログイン画面に戻してエラーを表示
        }
        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("errorMessage", "パスワードを入力してください。");
            return "index";
        }
        
        // 2. データベース（Aiven）からユーザーを検索して照合
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return "redirect:/tasks"; // 一致したら一覧へ
        } else {
            // バリデーションを通過しても、名前やパスワードがDBと違えばエラーにする
            model.addAttribute("errorMessage", "ユーザー名またはパスワードが正しくありません。");
            return "index";
        }
    }

    @GetMapping("/tasks")
    public String tasks(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "favoriteOnly", required = false, defaultValue = "false") boolean favoriteOnly,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "newest") String sortOrder,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            Model model) {
        
        List<Task> allTasks = taskRepository.findAll();
        
        long privateCount = allTasks.stream().filter(t -> "プライベート".equals(t.getCategory())).count();
        long workCount = allTasks.stream().filter(t -> "仕事".equals(t.getCategory())).count();
        long studyCount = allTasks.stream().filter(t -> "勉強".equals(t.getCategory())).count();
        
        model.addAttribute("privateCount", privateCount);
        model.addAttribute("workCount", workCount);
        model.addAttribute("studyCount", studyCount);

        List<Task> resultList = allTasks;

        if (keyword != null && !keyword.trim().isEmpty()) {
            resultList = resultList.stream()
                .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
            model.addAttribute("keyword", keyword);
        }

        if (favoriteOnly) {
            resultList = resultList.stream()
                .filter(t -> t.getIsFavorite() != null && t.getIsFavorite() == 1)
                .collect(Collectors.toList());
        }
        model.addAttribute("favoriteOnly", favoriteOnly);

        if (categoryName != null && !categoryName.trim().isEmpty()) {
            resultList = resultList.stream()
                .filter(t -> categoryName.equals(t.getCategory()))
                .collect(Collectors.toList());
            model.addAttribute("selectedCategory", categoryName);
        }

        resultList.sort((a, b) -> {
            int favComp = b.getIsFavorite().compareTo(a.getIsFavorite());
            if (favComp != 0) return favComp;
            
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            if ("oldest".equals(sortOrder)) {
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            } else {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
        });

        int totalTasks = resultList.size();
        int pageSize = 5;
        int totalPages = (int) Math.ceil((double) totalTasks / pageSize);
        
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;
        
        List<Task> pagedTasks = resultList.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        int startTaskNum = totalTasks == 0 ? 0 : (page * pageSize) + 1;
        int endTaskNum = Math.min((page + 1) * pageSize, totalTasks);
        
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("startTaskNum", startTaskNum);
        model.addAttribute("endTaskNum", endTaskNum);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("sortOrder", sortOrder);

        model.addAttribute("tasks", pagedTasks);
        return "tasks";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Task> allTasks = taskRepository.findAll();
        LocalDate today = LocalDate.now();
        
        long totalTasks = allTasks.size();
        
        long completedTasks = allTasks.stream()
                .filter(t -> "完了".equals(t.getStatus()))
                .count();
        
        double completionRate = totalTasks == 0 ? 0.0 : ((double) completedTasks / totalTasks) * 100;
        completionRate = Math.round(completionRate * 10) / 10.0;
        
        long todayCreatedTasks = allTasks.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().isEqual(today))
                .count();
        
        long todayCompletedTasks = allTasks.stream()
                .filter(t -> "完了".equals(t.getStatus()) && t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().isEqual(today))
                .count();
        
        long monthCreatedTasks = allTasks.stream()
                .filter(t -> t.getCreatedAt() != null 
                        && t.getCreatedAt().getYear() == today.getYear() 
                        && t.getCreatedAt().getMonth() == today.getMonth())
                .count();
        
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("completionRate", completionRate);
        model.addAttribute("todayCreatedTasks", todayCreatedTasks);
        model.addAttribute("todayCompletedTasks", todayCompletedTasks);
        model.addAttribute("monthCreatedTasks", monthCreatedTasks);
        
        long todoCount = allTasks.stream().filter(t -> "未着手".equals(t.getStatus())).count();
        long progressCount = allTasks.stream().filter(t -> "進行中".equals(t.getStatus())).count();
        
        double todoRate = totalTasks == 0 ? 0.0 : ((double) todoCount / totalTasks) * 100;
        double progressRate = totalTasks == 0 ? 0.0 : ((double) progressCount / totalTasks) * 100;
        
        long dashPrivateCount = allTasks.stream().filter(t -> "プライベート".equals(t.getCategory())).count();
        long dashWorkCount = allTasks.stream().filter(t -> "仕事".equals(t.getCategory())).count();
        long dashStudyCount = allTasks.stream().filter(t -> "勉強".equals(t.getCategory())).count();
        
        double privateRate = totalTasks == 0 ? 0.0 : ((double) dashPrivateCount / totalTasks) * 100;
        double workRate = totalTasks == 0 ? 0.0 : ((double) dashWorkCount / totalTasks) * 100;
        double studyRate = totalTasks == 0 ? 0.0 : ((double) dashStudyCount / totalTasks) * 100;
        
        model.addAttribute("todoCount", todoCount);
        model.addAttribute("todoRate", Math.round(todoRate * 10) / 10.0);
        model.addAttribute("progressCount", progressCount);
        model.addAttribute("progressRate", Math.round(progressRate * 10) / 10.0);
        
        model.addAttribute("dashPrivateCount", dashPrivateCount);
        model.addAttribute("privateRate", Math.round(privateRate * 10) / 10.0);
        model.addAttribute("dashWorkCount", dashWorkCount);
        model.addAttribute("workRate", Math.round(workRate * 10) / 10.0);
        model.addAttribute("dashStudyCount", dashStudyCount);
        model.addAttribute("studyRate", Math.round(studyRate * 10) / 10.0);
        
        List<String> dateLabels = new ArrayList<>();
        List<Long> dateCounts = new ArrayList<>();
        List<Double> dateRates = new ArrayList<>();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            dateLabels.add(targetDate.toString());
            long count = allTasks.stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().isEqual(targetDate))
                    .count();
            dateCounts.add(count);
            double rate = totalTasks == 0 ? 0.0 : ((double) count / totalTasks) * 100;
            dateRates.add(Math.round(rate * 10) / 10.0);
        }
        
        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("dateCounts", dateCounts);
        model.addAttribute("dateRates", dateRates);
        
        return "dashboard";
    }
        // ─── ここから後半部分が合体します ───
    @GetMapping("/tasks/create")
    public String create() {
        return "create";
    }

    @PostMapping("/tasks/add")
    public String addTask(@RequestParam("title") String title,
                          @RequestParam("category") String category,
                          @RequestParam("priority") String priority,
                          @RequestParam(value = "isFavorite", defaultValue = "0") Integer isFavorite) {
        Task task = new Task();
        task.setTitle(title);
        task.setCategory(category);
        task.setPriority(priority);
        task.setIsFavorite(isFavorite);
        task.setStatus("未着手"); 
        task.setCreatedAt(LocalDateTime.now()); 
        taskRepository.save(task);
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable("id") Long id) {
        taskRepository.deleteById(id);
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/edit/{id}")
    public String editTask(@PathVariable("id") Long id, Model model) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isPresent()) {
            model.addAttribute("task", taskOpt.get());
            return "edit";
        } else {
            return "redirect:/tasks";
        }
    }

    @PostMapping("/tasks/update")
    public String updateTask(@RequestParam("id") Long id,
                             @RequestParam("title") String title,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam("status") String status,
                             @RequestParam("priority") String priority,
                             @RequestParam(value = "category", required = false) String category,
                             @RequestParam(value = "newCategory", required = false) String newCategory) {
        
        Optional<Task> taskOpt = taskRepository.findById(id);
        
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.setTitle(title);
            task.setDescription(description);
            task.setStatus(status);
            task.setPriority(priority);
            
            if (newCategory != null && !newCategory.trim().isEmpty()) {
                task.setCategory(newCategory);
            } else {
                task.setCategory(category);
            }
            taskRepository.save(task);
        }
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/copy/{id}")
    public String copyTask(@PathVariable("id") Long id, Model model) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isPresent()) {
            Task original = taskOpt.get();
            Task cloneTask = new Task();
            cloneTask.setTitle("コピー - " + original.getTitle());
            cloneTask.setDescription(original.getDescription());
            cloneTask.setCategory(original.getCategory());
            cloneTask.setPriority(original.getPriority());
            cloneTask.setIsFavorite(0);
            cloneTask.setStatus("未着手");
            cloneTask.setCreatedAt(LocalDateTime.now()); 
            taskRepository.save(cloneTask);
        }
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/toggle-favorite/{id}")
    public String toggleFavorite(@PathVariable("id") Long id) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (task.getIsFavorite() == 1) {
                task.setIsFavorite(0);
            } else {
                task.setIsFavorite(1);
            }
            taskRepository.save(task);
        }
        return "redirect:/tasks";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}

